package com.capsule.insurance.analysis.application;

import com.capsule.insurance.analysis.dto.CoveragePercentileResponse;
import com.capsule.insurance.analysis.dto.DiagnosisReportResponse;
import com.capsule.insurance.analysis.dto.DiagnosisReportResponse.CategoryDiagnosis;
import com.capsule.insurance.analysis.dto.DiagnosisReportResponse.RecommendedProduct;
import com.capsule.insurance.auth.domain.Gender;
import com.capsule.insurance.auth.domain.UserAccount;
import com.capsule.insurance.auth.infra.UserAccountMapper;
import com.capsule.insurance.insurer.domain.CapsuleProduct;
import com.capsule.insurance.insurer.domain.CoverageCategory;
import com.capsule.insurance.insurer.dto.ProductSummaryResponse;
import com.capsule.insurance.insurer.infra.InsurerCatalogMapper;
import com.capsule.insurance.mydata.domain.ContractCoverageStatus;
import com.capsule.insurance.mydata.domain.MyDataContract;
import com.capsule.insurance.mydata.domain.MyDataContractCoverage;
import com.capsule.insurance.mydata.infra.MyDataContractMapper;
import com.capsule.insurance.subscription.domain.Subscription;
import com.capsule.insurance.subscription.domain.SubscriptionItem;
import com.capsule.insurance.subscription.infra.SubscriptionMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final List<CoverageCategory> DIAGNOSIS_CATEGORIES = List.of(
            CoverageCategory.DEATH,
            CoverageCategory.CANCER,
            CoverageCategory.BRAIN_HEART,
            CoverageCategory.ACTUAL_LOSS,
            CoverageCategory.SURGERY,
            CoverageCategory.ACCIDENT,
            CoverageCategory.LIABILITY
    );

    private final MyDataContractMapper myDataContractMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final InsurerCatalogMapper insurerCatalogMapper;
    private final UserAccountMapper userAccountMapper;

    public DiagnosisReportResponse getDiagnosisReport(Long userId) {
        CoverageSnapshot snapshot = collectCoverageSnapshot(userId);
        String gender = resolveGender(userId);

        List<CategoryDiagnosis> diagnoses = new ArrayList<>();
        for (CoverageCategory category : DIAGNOSIS_CATEGORIES) {
            boolean insured = snapshot.coveredCategories().contains(category);
            diagnoses.add(new CategoryDiagnosis(
                    category.name(),
                    toCategoryName(category),
                    insured,
                    insured ? "가입됨" : "미가입",
                    List.copyOf(snapshot.coverageNamesByCategory().getOrDefault(category, Set.of())),
                    insured ? null : pickRecommendedProduct(category, gender)
            ));
        }

        int coveredCount = snapshot.coveredCategories().size();
        int totalCategoryCount = DIAGNOSIS_CATEGORIES.size();
        String description = coveredCount == totalCategoryCount
                ? "7대 카테고리 모두 보장을 보유하고 있습니다."
                : "7대 카테고리 중 %d개 보장을 보유하고 있습니다. 미가입 카테고리에는 추천 상품 1건을 함께 제공합니다."
                .formatted(coveredCount);

        return new DiagnosisReportResponse(description, diagnoses);
    }

    public CoveragePercentileResponse getCoveragePercentile(Long userId) {
        CoverageSnapshot snapshot = collectCoverageSnapshot(userId);
        int totalCategoryCount = DIAGNOSIS_CATEGORIES.size();
        int coveredCategoryCount = snapshot.coveredCategories().size();
        int coveragePercentile = Math.round((coveredCategoryCount * 100.0f) / totalCategoryCount);

        return new CoveragePercentileResponse(
                coveragePercentile,
                coveredCategoryCount,
                totalCategoryCount,
                "%d대 카테고리 중 %d개를 보유하고 있습니다.".formatted(totalCategoryCount, coveredCategoryCount)
        );
    }

    private CoverageSnapshot collectCoverageSnapshot(Long userId) {
        EnumSet<CoverageCategory> coveredCategories = EnumSet.noneOf(CoverageCategory.class);
        Map<CoverageCategory, Set<String>> coverageNamesByCategory = new EnumMap<>(CoverageCategory.class);

        for (MyDataContract contract : myDataContractMapper.findContractsByUserId(userId)) {
            if (!isActiveContract(contract)) {
                continue;
            }
            for (MyDataContractCoverage coverage : contract.getCoverages()) {
                if (coverage.getCoverageStatus() != ContractCoverageStatus.NORMAL) {
                    continue;
                }
                CoverageCategory category = inferCoverageCategory(coverage.getCoverageCode(), coverage.getCoverageName());
                if (category == null || !DIAGNOSIS_CATEGORIES.contains(category)) {
                    continue;
                }
                coveredCategories.add(category);
                coverageNamesByCategory
                        .computeIfAbsent(category, key -> new LinkedHashSet<>())
                        .add(firstNonBlank(coverage.getCoverageName(), coverage.getCoverageCode()));
            }
        }

        Subscription subscription = subscriptionMapper.findSubscriptionAggregateByUserId(userId);
        if (subscription != null && subscription.getCurrentItems() != null) {
            for (SubscriptionItem item : subscription.getCurrentItems()) {
                CapsuleProduct capsuleProduct = insurerCatalogMapper.findCapsuleProductById(item.getCapsuleProductId());
                if (capsuleProduct == null || capsuleProduct.getCoverageCategory() == null) {
                    continue;
                }
                CoverageCategory category = capsuleProduct.getCoverageCategory();
                if (!DIAGNOSIS_CATEGORIES.contains(category)) {
                    continue;
                }
                coveredCategories.add(category);
                coverageNamesByCategory
                        .computeIfAbsent(category, key -> new LinkedHashSet<>())
                        .add(firstNonBlank(capsuleProduct.getProductName(), capsuleProduct.getCoverageName(), category.name()));
            }
        }

        return new CoverageSnapshot(coveredCategories, coverageNamesByCategory);
    }

    private RecommendedProduct pickRecommendedProduct(CoverageCategory category, String gender) {
        List<ProductSummaryResponse> candidates = insurerCatalogMapper.findProductSourcesByFilter(category.name(), null, gender);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        ProductSummaryResponse selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        return new RecommendedProduct(
                selected.productSourceId(),
                selected.companyName(),
                selected.productName(),
                selected.monthlyPrice()
        );
    }

    private boolean isActiveContract(MyDataContract contract) {
        return contract.getContractStatusCode() == null || "NORMAL".equalsIgnoreCase(contract.getContractStatusCode());
    }

    private String resolveGender(Long userId) {
        UserAccount user = userAccountMapper.findByUserId(userId);
        if (user == null || user.getGender() == null || user.getGender() == Gender.UNKNOWN) {
            return "M";
        }
        return user.getGender().name();
    }

    private CoverageCategory inferCoverageCategory(String coverageCode, String coverageName) {
        String normalized = (firstNonBlank(coverageCode, "") + " " + firstNonBlank(coverageName, ""))
                .toUpperCase(Locale.ROOT);

        if (containsAny(normalized, "DEATH", "사망")) {
            return CoverageCategory.DEATH;
        }
        if (containsAny(normalized, "CANCER", "암")) {
            return CoverageCategory.CANCER;
        }
        if (containsAny(normalized, "BRAIN", "HEART", "STROKE", "CARDIO", "DEMENTIA", "뇌", "심장", "치매")) {
            return CoverageCategory.BRAIN_HEART;
        }
        if (containsAny(normalized, "ACTUAL", "LOSS", "MEDICAL", "HOSPITAL", "실손", "의료", "입원", "통원")) {
            return CoverageCategory.ACTUAL_LOSS;
        }
        if (containsAny(normalized, "SURGERY", "OPERATION", "수술")) {
            return CoverageCategory.SURGERY;
        }
        if (containsAny(normalized, "LIABILITY", "배상")) {
            return CoverageCategory.LIABILITY;
        }
        if (containsAny(normalized, "ACCIDENT", "INJURY", "TRAUMA", "상해", "재해")) {
            return CoverageCategory.ACCIDENT;
        }
        return null;
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String toCategoryName(CoverageCategory category) {
        return switch (category) {
            case DEATH -> "사망";
            case CANCER -> "암";
            case BRAIN_HEART -> "뇌/심장";
            case ACTUAL_LOSS -> "실손";
            case SURGERY -> "수술";
            case ACCIDENT -> "상해";
            case LIABILITY -> "배상책임";
            case ETC -> "기타";
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private record CoverageSnapshot(
            Set<CoverageCategory> coveredCategories,
            Map<CoverageCategory, Set<String>> coverageNamesByCategory
    ) {
    }
}
