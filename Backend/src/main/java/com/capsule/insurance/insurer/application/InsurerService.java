package com.capsule.insurance.insurer.application;

import com.capsule.insurance.auth.domain.Gender;
import com.capsule.insurance.auth.domain.UserAccount;
import com.capsule.insurance.auth.infra.UserAccountMapper;
import com.capsule.insurance.insurer.infra.InsurerCatalogMapper;
import com.capsule.insurance.insurer.dto.ProductDetailResponse;
import com.capsule.insurance.insurer.dto.CategoryRecommendResponse;
import com.capsule.insurance.insurer.dto.ProductSummaryPageResponse;
import com.capsule.insurance.insurer.dto.ProductSummaryResponse;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.insurer.domain.ProductSource;
import com.capsule.insurance.insurer.dto.ProductSourceLightSummaryResponse;
import com.capsule.insurance.insurer.dto.ProductSourceTermsSummaryResponse;
import com.capsule.insurance.insurer.infra.ProductSourceMapper;
import com.capsule.insurance.insurer.infra.projection.PopularProductProjection;
import com.capsule.insurance.insurer.infra.projection.ProductSourceDetailProjection;
import com.capsule.insurance.insurer.infra.projection.ProductSourceSummaryProjection;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class InsurerService {

    private static final String TERMS_DISCLAIMER = "이 요약은 product_source 컬럼 기반 안내이며 실제 보장 제외·면책·지급 조건은 약관 원문을 확인해야 합니다.";
    private static final String LIGHT_TERMS_DISCLAIMER = "이 요약은 핵심 3가지만 빠르게 보여주는 light 버전 안내이며, 세부 조건은 전체 약관 요약 또는 원문을 확인해야 합니다.";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final int DEFAULT_PRODUCTS_PAGE_SIZE = 12;
    private static final int MAX_PRODUCTS_PAGE_SIZE = 30;

    private final ProductSourceMapper productSourceMapper;
    private final InsurerCatalogMapper insurerCatalogMapper;
    private final UserAccountMapper userAccountMapper;
    private final ChatClient chatClient;
    private final String openAiApiKey;

    public InsurerService(
            ProductSourceMapper productSourceMapper,
            InsurerCatalogMapper insurerCatalogMapper,
            UserAccountMapper userAccountMapper,
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            @Value("${spring.ai.openai.api-key:}") String openAiApiKey
    ) {
        this.productSourceMapper = productSourceMapper;
        this.insurerCatalogMapper = insurerCatalogMapper;
        this.userAccountMapper = userAccountMapper;
        ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = chatClientBuilder == null ? null : chatClientBuilder.build();
        this.openAiApiKey = openAiApiKey;
    }

    public ProductSummaryPageResponse getProducts(String category, Integer budget, Integer page, Integer size, Long userId) {
        String gender = resolveGender(userId);
        BigDecimal maxPrice = (budget != null) ? BigDecimal.valueOf(budget) : null;
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 ? DEFAULT_PRODUCTS_PAGE_SIZE : Math.min(size, MAX_PRODUCTS_PAGE_SIZE);
        int offset = safePage * safeSize;

        long totalElements = insurerCatalogMapper.countProductSourcesByFilter(category, maxPrice, gender, userId);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);

        List<ProductSummaryResponse> items = insurerCatalogMapper
                .findProductSourcesByFilterPaged(category, maxPrice, gender, userId, safeSize, offset)
                .stream()
                .map(this::toProductSummaryResponse)
                .toList();

        return new ProductSummaryPageResponse(
                items,
                safePage,
                safeSize,
                totalElements,
                totalPages,
                safePage + 1 < totalPages,
                safePage > 0
        );
    }

    public ProductDetailResponse getProductDetail(Long productSourceId, Long userId) {
        String gender = resolveGender(userId);
        ProductSourceDetailProjection detail = insurerCatalogMapper.findProductSourceDetail(productSourceId, gender);
        if (detail == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 product_source를 찾을 수 없습니다.");
        }
        return toProductDetailResponse(detail);
    }

    public ProductSourceTermsSummaryResponse getProductSourceTermsSummary(Long productSourceId) {
        ProductSource productSource = getProductSourceOrThrow(productSourceId);

        ProductSourceTermsSummaryResponse.PriceComparison priceComparison = buildPriceComparison(productSource);
        AiTermsSummary aiTermsSummary = generateTermsSummary(productSource, priceComparison);

        return new ProductSourceTermsSummaryResponse(
                productSource.getProductSourceId(),
                valueOrInfo(productSource.getCompanyName()),
                valueOrInfo(productSource.getProductName()),
                valueOrInfo(productSource.getSaleChannel()),
                valueOrInfo(productSource.getCoverageName()),
                valueOrInfo(aiTermsSummary.headline()),
                normalizeHighlights(aiTermsSummary.clauseHighlights(), productSource, priceComparison),
                valueOrInfo(aiTermsSummary.coverageSummary()),
                valueOrInfo(aiTermsSummary.subscriptionConditions()),
                valueOrInfo(aiTermsSummary.premiumAndPriceIndex()),
                valueOrInfo(aiTermsSummary.refundAndInterest()),
                valueOrInfo(aiTermsSummary.specialNotes()),
                priceComparison,
                TERMS_DISCLAIMER
        );
    }

    public ProductSourceLightSummaryResponse getProductSourceLightSummary(Long productSourceId) {
        ProductSource productSource = getProductSourceOrThrow(productSourceId);

        ProductSourceTermsSummaryResponse.PriceComparison priceComparison = buildPriceComparison(productSource);
        AiLightSummary aiLightSummary = generateLightSummary(productSource, priceComparison);

        return new ProductSourceLightSummaryResponse(
                productSource.getProductSourceId(),
                valueOrInfo(productSource.getCompanyName()),
                valueOrInfo(productSource.getProductName()),
                valueOrInfo(aiLightSummary.paymentSummary()),
                valueOrInfo(aiLightSummary.coverageSummary()),
                valueOrInfo(aiLightSummary.featureSummary()),
                LIGHT_TERMS_DISCLAIMER
        );
    }

    public List<CategoryRecommendResponse> getCategoryRecommendations(Long userId) {
        String gender = resolveGender(userId);
        List<String> categoryCodes = List.of("CANCER", "DEATH", "SURGERY");
        try {
            List<String> onboardingCategories = userAccountMapper.findOnboardingCategoriesByUserId(userId);
            if (onboardingCategories != null && !onboardingCategories.isEmpty()) {
                categoryCodes = onboardingCategories;
            }
        } catch (Exception exception) {
            log.warn("Failed to load onboarding categories. default categories will be used. userId={}", userId, exception);
        }

        try {
            List<PopularProductProjection> candidates =
                    insurerCatalogMapper.findPopularProductsByCategories(categoryCodes, gender, userId);
            return mixCategoryRecommendations(candidates, categoryCodes, 5);
        } catch (Exception exception) {
            log.warn("Category recommendations query failed. fallback will be used. userId={}, categories={}",
                    userId, categoryCodes, exception);
            return buildFallbackCategoryRecommendations(categoryCodes, gender, userId, 5);
        }
    }

    private String resolveGender(Long userId) {
        try {
            UserAccount user = userAccountMapper.findByUserId(userId);
            if (user != null && user.getGender() != null && user.getGender() != Gender.UNKNOWN) {
                return user.getGender().name();
            }
        } catch (Exception exception) {
            log.debug("Failed to resolve gender for userId={}", userId, exception);
        }
        return "M";
    }

    private ProductSource getProductSourceOrThrow(Long productSourceId) {
        ProductSource productSource = productSourceMapper.findById(productSourceId);
        if (productSource == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 product_source를 찾을 수 없습니다.");
        }
        return productSource;
    }

    private ProductSummaryResponse toProductSummaryResponse(ProductSourceSummaryProjection projection) {
        return new ProductSummaryResponse(
                projection.productSourceId(),
                projection.companyName(),
                projection.productName(),
                projection.insurerSector(),
                projection.coverageCategoryCode(),
                projection.coverageCode(),
                projection.monthlyPrice(),
                projection.termsUri(),
                projection.loadedAt(),
                projection.updatedAt()
        );
    }

    private List<CategoryRecommendResponse> buildFallbackCategoryRecommendations(
            List<String> categoryCodes,
            String gender,
            Long userId,
            int maxItems
    ) {
        if (categoryCodes == null || categoryCodes.isEmpty()) {
            return List.of();
        }

        List<CategoryRecommendResponse> results = new ArrayList<>();
        for (String categoryCode : categoryCodes) {
            List<ProductSourceSummaryProjection> products;
            try {
                products = insurerCatalogMapper.findProductSourcesByFilter(
                        categoryCode,
                        null,
                        gender,
                        userId);
            } catch (Exception exception) {
                log.warn("Fallback recommendation query failed for category={}", categoryCode, exception);
                continue;
            }

            if (products == null || products.isEmpty()) {
                continue;
            }

            for (ProductSourceSummaryProjection product : products) {
                if (results.size() >= maxItems) {
                    break;
                }
                results.add(new CategoryRecommendResponse(
                        product.productSourceId(),
                        product.companyName(),
                        product.productName(),
                        product.coverageCategoryCode(),
                        product.monthlyPrice(),
                        0L));
            }

            if (results.size() >= maxItems) {
                break;
            }
        }
        return results;
    }

    private List<CategoryRecommendResponse> mixCategoryRecommendations(
            List<PopularProductProjection> candidates,
            List<String> categoryCodes,
            int maxItems
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Map<String, List<PopularProductProjection>> byCategory = new LinkedHashMap<>();
        for (String categoryCode : categoryCodes) {
            byCategory.put(categoryCode, new ArrayList<>());
        }
        for (PopularProductProjection candidate : candidates) {
            byCategory.computeIfAbsent(candidate.coverageCategoryCode(), key -> new ArrayList<>()).add(candidate);
        }

        List<CategoryRecommendResponse> results = new ArrayList<>();
        int round = 0;
        while (results.size() < maxItems) {
            boolean addedInRound = false;
            for (String categoryCode : byCategory.keySet()) {
                List<PopularProductProjection> list = byCategory.get(categoryCode);
                if (list == null || round >= list.size()) {
                    continue;
                }
                PopularProductProjection picked = list.get(round);
                results.add(new CategoryRecommendResponse(
                        picked.productSourceId(),
                        picked.companyName(),
                        picked.productName(),
                        picked.coverageCategoryCode(),
                        picked.monthlyPrice(),
                        picked.subscriberCount()));
                addedInRound = true;
                if (results.size() >= maxItems) {
                    break;
                }
            }
            if (!addedInRound) {
                break;
            }
            round++;
        }
        return results;
    }

    private ProductDetailResponse toProductDetailResponse(ProductSourceDetailProjection projection) {
        String prioritizedCoverageAmount = firstNonBlank(projection.joinAmount(), projection.payoutAmount());

        return new ProductDetailResponse(
                projection.productSourceId(),
                projection.companyName(),
                projection.productName(),
                projection.insurerSector(),
                projection.saleChannel(),
                projection.coverageName(),
                projection.claimReason(),
                prioritizedCoverageAmount,
                projection.joinAmount(),
                projection.minimumJoinPremium(),
                projection.paymentCycle(),
                projection.paymentTerm(),
                projection.coverageTerm(),
                projection.coverageCategoryCode(),
                projection.coverageCode(),
                projection.monthlyPrice(),
                projection.productSummary(),
                projection.productFeature(),
                projection.specialNote(),
                projection.contactPhone(),
                projection.saleDate(),
                projection.currentAnnouncedRate(),
                projection.fixedRate(),
                projection.minimumGuaranteedRate()
        );
    }

    private AiTermsSummary generateTermsSummary(
            ProductSource productSource,
            ProductSourceTermsSummaryResponse.PriceComparison priceComparison
    ) {
        AiTermsSummary fallback = buildFallbackSummary(productSource, priceComparison);

        if (chatClient == null || !StringUtils.hasText(openAiApiKey)) {
            return fallback;
        }

        try {
            AiTermsSummary aiTermsSummary = chatClient.prompt()
                    .system("""
                            너는 보험 약관 요약 도우미다.
                            입력으로 전달된 product_source 컬럼 값만 사용해서 작성한다.
                            존재하지 않는 보장, 면책, 예외, 세부 지급조건은 추정하지 않는다.
                            문장은 약관 요약처럼 차분하고 간결하게 작성한다.
                            clauseHighlights는 반드시 3개 항목으로 작성한다.
                            premiumAndPriceIndex에는 남성/여성 보험료와 보험가격지수를 모두 언급하고,
                            보험가격지수가 더 낮은 쪽이 상대적으로 유리하다는 설명을 반영한다.
                            specialNotes에는 상품특징, 특이사항, 갱신형 여부, 유니버설 여부, 문의처 중 있는 정보만 반영한다.
                            모든 필드는 한국어로 작성하고, 값이 없으면 '정보 없음'이라고 적는다.
                            """)
                    .user(buildPrompt(productSource, priceComparison))
                    .call()
                    .entity(AiTermsSummary.class);

            return mergeWithFallback(aiTermsSummary, fallback, productSource, priceComparison);
        } catch (Exception exception) {
            log.warn("AI terms summary generation failed for productSourceId={}", productSource.getProductSourceId(), exception);
            return fallback;
        }
    }

    private AiLightSummary generateLightSummary(
            ProductSource productSource,
            ProductSourceTermsSummaryResponse.PriceComparison priceComparison
    ) {
        AiLightSummary fallback = buildFallbackLightSummary(productSource, priceComparison);

        if (chatClient == null || !StringUtils.hasText(openAiApiKey)) {
            return fallback;
        }

        try {
            AiLightSummary aiLightSummary = chatClient.prompt()
                    .system("""
                            너는 보험 핵심 요약 도우미다.
                            입력으로 전달된 product_source 컬럼 값만 사용해서 작성한다.
                            이미지에 나온 3가지 질문에만 답한다.
                            1. 결국 얼마를 내는지
                            2. 언제, 무엇을 보장해 주는지
                            3. 이 상품의 특징이 무엇인지
                            각 항목은 1~2문장 이내로 짧고 명확하게 작성한다.
                            존재하지 않는 보장, 면책, 예외, 세부 조건은 추정하지 않는다.
                            모든 필드는 한국어로 작성하고, 값이 없으면 '정보 없음'이라고 적는다.
                            """)
                    .user(buildLightPrompt(productSource, priceComparison))
                    .call()
                    .entity(AiLightSummary.class);

            if (aiLightSummary == null) {
                return fallback;
            }

            return new AiLightSummary(
                    valueOrDefault(aiLightSummary.paymentSummary(), fallback.paymentSummary()),
                    valueOrDefault(aiLightSummary.coverageSummary(), fallback.coverageSummary()),
                    valueOrDefault(aiLightSummary.featureSummary(), fallback.featureSummary())
            );
        } catch (Exception exception) {
            log.warn("AI light summary generation failed for productSourceId={}", productSource.getProductSourceId(), exception);
            return fallback;
        }
    }

    private String buildPrompt(
            ProductSource productSource,
            ProductSourceTermsSummaryResponse.PriceComparison priceComparison
    ) {
        return """
                아래 보험상품 정보를 product_source 기반 약관 요약 형식으로 정리해줘.
                출력 구조는 시스템이 강제하는 형식을 따르고, 과장 없이 사실만 정리해.

                [상품 기본]
                회사명: %s
                상품명: %s
                판매채널: %s
                계약형태: %s
                보장명: %s
                보험금 지급 사유: %s
                지급 금액: %s
                가입 금액: %s
                최소 가입 보험료: %s

                [납입/보장]
                납입 주기: %s
                납입 기간: %s
                보장 기간: %s
                예상 갱신 보험료: %s
                갱신형 여부: %s
                유니버설 여부: %s

                [남녀 보험료/가격지수]
                남성 월 보험료: %s
                여성 월 보험료: %s
                남성 보험가격지수: %s
                여성 보험가격지수: %s
                상대적으로 유리한 성별: %s
                비교 설명: %s

                [이율/환급]
                고정이율: %s
                공시이율: %s
                최저보증이율: %s
                보장부분 적용이율: %s
                적립부분 적용이율: %s
                해약환급금: %s
                최저 사망보험금: %s
                최저 사망보험금 산출방식: %s
                최저 해약환급금: %s
                최저 해약환급금 산출방식: %s

                [부가 정보]
                상품 요약: %s
                상품 특징: %s
                특이사항: %s
                경증치매 보장 여부: %s
                경증치매 보장 금액: %s
                문의처: %s
                """.formatted(
                valueOrInfo(productSource.getCompanyName()),
                valueOrInfo(productSource.getProductName()),
                valueOrInfo(productSource.getSaleChannel()),
                valueOrInfo(productSource.getContractTypeText()),
                valueOrInfo(productSource.getCoverageName()),
                valueOrInfo(limitText(productSource.getClaimReasonText(), 700)),
                valueOrInfo(limitText(productSource.getPayoutAmountText(), 500)),
                valueOrInfo(limitText(productSource.getJoinAmountText(), 500)),
                valueOrInfo(limitText(productSource.getMinimumJoinPremiumText(), 300)),
                valueOrInfo(productSource.getPaymentCycle()),
                valueOrInfo(productSource.getPaymentTerm()),
                valueOrInfo(productSource.getCoverageTerm()),
                valueOrInfo(limitText(productSource.getExpectedRenewalPremiumText(), 300)),
                valueOrInfo(productSource.getRenewalText()),
                valueOrInfo(productSource.getUniversalText()),
                valueOrInfo(priceComparison.maleMonthlyPremium()),
                valueOrInfo(priceComparison.femaleMonthlyPremium()),
                valueOrInfo(priceComparison.malePriceIndex()),
                valueOrInfo(priceComparison.femalePriceIndex()),
                valueOrInfo(priceComparison.advantageousGender()),
                valueOrInfo(priceComparison.advantageousReason()),
                valueOrInfo(productSource.getFixedRateText()),
                valueOrInfo(productSource.getCurrentAnnouncedRateText()),
                valueOrInfo(productSource.getMinimumGuaranteedRateText()),
                valueOrInfo(productSource.getCoveragePartInterestRateText()),
                valueOrInfo(productSource.getReservePartInterestRateText()),
                valueOrInfo(limitText(productSource.getSurrenderValueText(), 500)),
                valueOrInfo(limitText(productSource.getMinimumDeathBenefitText(), 300)),
                valueOrInfo(limitText(productSource.getMinimumDeathBenefitMethodText(), 300)),
                valueOrInfo(limitText(productSource.getMinimumSurrenderValueText(), 300)),
                valueOrInfo(limitText(productSource.getMinimumSurrenderValueMethodText(), 300)),
                valueOrInfo(limitText(productSource.getProductSummaryText(), 800)),
                valueOrInfo(limitText(productSource.getProductFeatureText(), 900)),
                valueOrInfo(limitText(productSource.getSpecialNote(), 900)),
                valueOrInfo(productSource.getMildDementiaCoveredText()),
                valueOrInfo(productSource.getMildDementiaBenefitAmountText()),
                valueOrInfo(productSource.getContactPhone())
        );
    }

    private String buildLightPrompt(
            ProductSource productSource,
            ProductSourceTermsSummaryResponse.PriceComparison priceComparison
    ) {
        return """
                아래 보험상품 정보를 보고, 딱 3가지 질문에만 답해줘.

                [질문 1] 결국 얼마를 내는지
                - 남성 월 보험료: %s
                - 여성 월 보험료: %s
                - 남성 보험가격지수: %s
                - 여성 보험가격지수: %s
                - 상대적으로 유리한 성별: %s
                - 비교 설명: %s
                - 최소 가입 보험료: %s
                - 예상 갱신 보험료: %s

                [질문 2] 언제, 무엇을 보장해 주는지
                - 보장명: %s
                - 보험금 지급 사유: %s
                - 지급 금액: %s
                - 가입 금액: %s
                - 보장 기간: %s
                - 계약 형태: %s

                [질문 3] 이 상품의 특징이 무엇인지
                - 상품 요약: %s
                - 상품 특징: %s
                - 특이사항: %s
                - 갱신형 여부: %s
                - 유니버설 여부: %s
                - 판매채널: %s
                - 문의처: %s
                """.formatted(
                valueOrInfo(priceComparison.maleMonthlyPremium()),
                valueOrInfo(priceComparison.femaleMonthlyPremium()),
                valueOrInfo(priceComparison.malePriceIndex()),
                valueOrInfo(priceComparison.femalePriceIndex()),
                valueOrInfo(priceComparison.advantageousGender()),
                valueOrInfo(priceComparison.advantageousReason()),
                valueOrInfo(productSource.getMinimumJoinPremiumText()),
                valueOrInfo(productSource.getExpectedRenewalPremiumText()),
                valueOrInfo(productSource.getCoverageName()),
                valueOrInfo(limitText(productSource.getClaimReasonText(), 500)),
                valueOrInfo(limitText(productSource.getPayoutAmountText(), 300)),
                valueOrInfo(limitText(productSource.getJoinAmountText(), 300)),
                valueOrInfo(productSource.getCoverageTerm()),
                valueOrInfo(productSource.getContractTypeText()),
                valueOrInfo(limitText(productSource.getProductSummaryText(), 600)),
                valueOrInfo(limitText(productSource.getProductFeatureText(), 700)),
                valueOrInfo(limitText(productSource.getSpecialNote(), 700)),
                valueOrInfo(productSource.getRenewalText()),
                valueOrInfo(productSource.getUniversalText()),
                valueOrInfo(productSource.getSaleChannel()),
                valueOrInfo(productSource.getContactPhone())
        );
    }

    private AiTermsSummary mergeWithFallback(
            AiTermsSummary aiTermsSummary,
            AiTermsSummary fallback,
            ProductSource productSource,
            ProductSourceTermsSummaryResponse.PriceComparison priceComparison
    ) {
        if (aiTermsSummary == null) {
            return fallback;
        }

        return new AiTermsSummary(
                valueOrDefault(aiTermsSummary.headline(), fallback.headline()),
                normalizeHighlights(aiTermsSummary.clauseHighlights(), productSource, priceComparison),
                valueOrDefault(aiTermsSummary.coverageSummary(), fallback.coverageSummary()),
                valueOrDefault(aiTermsSummary.subscriptionConditions(), fallback.subscriptionConditions()),
                valueOrDefault(aiTermsSummary.premiumAndPriceIndex(), fallback.premiumAndPriceIndex()),
                valueOrDefault(aiTermsSummary.refundAndInterest(), fallback.refundAndInterest()),
                valueOrDefault(aiTermsSummary.specialNotes(), fallback.specialNotes())
        );
    }

    private AiTermsSummary buildFallbackSummary(
            ProductSource productSource,
            ProductSourceTermsSummaryResponse.PriceComparison priceComparison
    ) {
        return new AiTermsSummary(
                buildHeadline(productSource),
                buildFallbackHighlights(productSource, priceComparison),
                joinSentences(
                        sentence("주요 보장", productSource.getCoverageName()),
                        sentence("보험금 지급 사유", productSource.getClaimReasonText()),
                        sentence("보험금 수준", productSource.getPayoutAmountText()),
                        sentence("가입 금액", productSource.getJoinAmountText()),
                        sentence("상품 요약", productSource.getProductSummaryText())
                ),
                joinSentences(
                        sentence("판매 채널", productSource.getSaleChannel()),
                        sentence("계약 형태", productSource.getContractTypeText()),
                        sentence("납입 주기", productSource.getPaymentCycle()),
                        sentence("납입 기간", productSource.getPaymentTerm()),
                        sentence("보장 기간", productSource.getCoverageTerm()),
                        sentence("최소 가입 보험료", productSource.getMinimumJoinPremiumText()),
                        sentence("예상 갱신 보험료", productSource.getExpectedRenewalPremiumText())
                ),
                joinSentences(
                        sentence("남성 월 보험료", priceComparison.maleMonthlyPremium()),
                        sentence("여성 월 보험료", priceComparison.femaleMonthlyPremium()),
                        sentence("남성 보험가격지수", priceComparison.malePriceIndex()),
                        sentence("여성 보험가격지수", priceComparison.femalePriceIndex()),
                        priceComparison.advantageousReason()
                ),
                joinSentences(
                        sentence("고정이율", productSource.getFixedRateText()),
                        sentence("공시이율", productSource.getCurrentAnnouncedRateText()),
                        sentence("최저보증이율", productSource.getMinimumGuaranteedRateText()),
                        sentence("보장부분 적용이율", productSource.getCoveragePartInterestRateText()),
                        sentence("적립부분 적용이율", productSource.getReservePartInterestRateText()),
                        sentence("해약환급금", productSource.getSurrenderValueText()),
                        sentence("최저 사망보험금", productSource.getMinimumDeathBenefitText()),
                        sentence("최저 사망보험금 산출방식", productSource.getMinimumDeathBenefitMethodText()),
                        sentence("최저 해약환급금", productSource.getMinimumSurrenderValueText()),
                        sentence("최저 해약환급금 산출방식", productSource.getMinimumSurrenderValueMethodText())
                ),
                joinSentences(
                        sentence("상품 특징", productSource.getProductFeatureText()),
                        sentence("특이사항", productSource.getSpecialNote()),
                        sentence("갱신형 여부", productSource.getRenewalText()),
                        sentence("유니버설 여부", productSource.getUniversalText()),
                        sentence("경증치매 보장 여부", productSource.getMildDementiaCoveredText()),
                        sentence("경증치매 보장 금액", productSource.getMildDementiaBenefitAmountText()),
                        sentence("문의처", productSource.getContactPhone())
                )
        );
    }

    private AiLightSummary buildFallbackLightSummary(
            ProductSource productSource,
            ProductSourceTermsSummaryResponse.PriceComparison priceComparison
    ) {
        return new AiLightSummary(
                joinSentences(
                        sentence("남성 월 보험료", priceComparison.maleMonthlyPremium()),
                        sentence("여성 월 보험료", priceComparison.femaleMonthlyPremium()),
                        sentence("남성 보험가격지수", priceComparison.malePriceIndex()),
                        sentence("여성 보험가격지수", priceComparison.femalePriceIndex()),
                        priceComparison.advantageousReason()
                ),
                joinSentences(
                        sentence("보장명", productSource.getCoverageName()),
                        sentence("보험금 지급 사유", productSource.getClaimReasonText()),
                        sentence("지급 금액", productSource.getPayoutAmountText()),
                        sentence("보장 기간", productSource.getCoverageTerm())
                ),
                joinSentences(
                        sentence("상품 요약", productSource.getProductSummaryText()),
                        sentence("상품 특징", productSource.getProductFeatureText()),
                        sentence("특이사항", productSource.getSpecialNote()),
                        sentence("갱신형 여부", productSource.getRenewalText()),
                        sentence("유니버설 여부", productSource.getUniversalText())
                )
        );
    }

    private ProductSourceTermsSummaryResponse.PriceComparison buildPriceComparison(ProductSource productSource) {
        String malePremium = resolvePremium(productSource.getMonthlyPremiumMale(), productSource.getPremiumMaleText());
        String femalePremium = resolvePremium(productSource.getMonthlyPremiumFemale(), productSource.getPremiumFemaleText());
        String malePriceIndex = valueOrInfo(cleanText(productSource.getPriceIndexMaleText()));
        String femalePriceIndex = valueOrInfo(cleanText(productSource.getPriceIndexFemaleText()));

        BigDecimal maleIndexValue = parseDecimal(productSource.getPriceIndexMaleText());
        BigDecimal femaleIndexValue = parseDecimal(productSource.getPriceIndexFemaleText());

        String advantageousGender = "정보 없음";
        String advantageousReason = "남녀 비교에 필요한 보험가격지수 정보가 충분하지 않습니다.";

        if (maleIndexValue != null && femaleIndexValue != null) {
            int comparison = maleIndexValue.compareTo(femaleIndexValue);
            if (comparison < 0) {
                advantageousGender = "남성";
                advantageousReason = "보험가격지수 기준으로 남성 지수가 더 낮아 상대적으로 유리합니다.";
            } else if (comparison > 0) {
                advantageousGender = "여성";
                advantageousReason = "보험가격지수 기준으로 여성 지수가 더 낮아 상대적으로 유리합니다.";
            } else {
                advantageousGender = "유사";
                advantageousReason = "남녀 보험가격지수가 동일하거나 유사해 한쪽이 특별히 더 유리하다고 보기 어렵습니다.";
            }
        }

        return new ProductSourceTermsSummaryResponse.PriceComparison(
                malePremium,
                femalePremium,
                malePriceIndex,
                femalePriceIndex,
                advantageousGender,
                advantageousReason
        );
    }

    private String buildHeadline(ProductSource productSource) {
        String companyName = valueOrInfo(productSource.getCompanyName());
        String productName = valueOrInfo(productSource.getProductName());
        String coverageName = cleanText(productSource.getCoverageName());

        if (StringUtils.hasText(coverageName)) {
            return companyName + "의 " + productName + "으로, " + coverageName + " 중심 보장을 안내하는 상품입니다.";
        }

        return companyName + "의 " + productName + " 상품 요약입니다.";
    }

    private List<String> normalizeHighlights(
            List<String> aiHighlights,
            ProductSource productSource,
            ProductSourceTermsSummaryResponse.PriceComparison priceComparison
    ) {
        List<String> normalized = new ArrayList<>();
        if (aiHighlights != null) {
            for (String highlight : aiHighlights) {
                String cleaned = cleanText(highlight);
                if (StringUtils.hasText(cleaned) && !normalized.contains(cleaned)) {
                    normalized.add(cleaned);
                }
                if (normalized.size() == 3) {
                    break;
                }
            }
        }

        if (normalized.size() < 3) {
            for (String highlight : buildFallbackHighlights(productSource, priceComparison)) {
                if (!normalized.contains(highlight)) {
                    normalized.add(highlight);
                }
                if (normalized.size() == 3) {
                    break;
                }
            }
        }

        while (normalized.size() < 3) {
            normalized.add("추가 핵심 정보는 상품 원문을 확인해야 합니다.");
        }

        return List.copyOf(normalized);
    }

    private List<String> buildFallbackHighlights(
            ProductSource productSource,
            ProductSourceTermsSummaryResponse.PriceComparison priceComparison
    ) {
        List<String> highlights = new ArrayList<>();
        highlights.add(firstNonBlank(
                withSuffix(cleanText(productSource.getCoverageName()), " 보장 중심"),
                "주요 보장 내용 확인 필요"
        ));
        highlights.add(firstNonBlank(
                joinNonBlank(" / ",
                        withLabel(productSource.getPaymentTerm(), "납입"),
                        withLabel(productSource.getCoverageTerm(), "보장")),
                "납입·보장 기간 확인 필요"
        ));
        highlights.add(firstNonBlank(
                cleanText(priceComparison.advantageousReason()),
                "남녀 가격지수 비교 정보 확인 필요"
        ));
        return List.copyOf(highlights);
    }

    private String resolvePremium(BigDecimal monthlyPremium, String premiumText) {
        if (monthlyPremium != null) {
            return formatWon(monthlyPremium);
        }
        return valueOrInfo(cleanText(premiumText));
    }

    private BigDecimal parseDecimal(String value) {
        String cleaned = cleanText(value);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(cleaned.replace(",", ""));
        if (!matcher.find()) {
            return null;
        }

        try {
            return new BigDecimal(matcher.group());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String sentence(String label, String value) {
        String cleaned = cleanText(value);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        return label + "은(는) " + cleaned + "입니다.";
    }

    private String joinSentences(String... sentences) {
        List<String> collected = new ArrayList<>();
        for (String sentence : sentences) {
            String cleaned = cleanText(sentence);
            if (StringUtils.hasText(cleaned)) {
                collected.add(cleaned);
            }
        }
        return collected.isEmpty() ? "정보 없음" : String.join(" ", collected);
    }

    private String formatWon(BigDecimal amount) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);
        return numberFormat.format(amount) + "원";
    }

    private String valueOrDefault(String value, String defaultValue) {
        String cleaned = cleanText(value);
        return StringUtils.hasText(cleaned) ? cleaned : defaultValue;
    }

    private String valueOrInfo(String value) {
        String cleaned = cleanText(value);
        return StringUtils.hasText(cleaned) ? cleaned : "정보 없음";
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String limitText(String value, int maxLength) {
        String cleaned = cleanText(value);
        if (!StringUtils.hasText(cleaned) || cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength) + "...";
    }

    private String withSuffix(String value, String suffix) {
        return StringUtils.hasText(value) ? value + suffix : null;
    }

    private String withLabel(String value, String label) {
        String cleaned = cleanText(value);
        return StringUtils.hasText(cleaned) ? label + " " + cleaned : null;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            String cleaned = cleanText(candidate);
            if (StringUtils.hasText(cleaned)) {
                return cleaned;
            }
        }
        return "정보 없음";
    }

    private String joinNonBlank(String delimiter, String... values) {
        List<String> collected = new ArrayList<>();
        for (String value : values) {
            String cleaned = cleanText(value);
            if (StringUtils.hasText(cleaned)) {
                collected.add(cleaned);
            }
        }
        return collected.isEmpty() ? null : String.join(delimiter, collected);
    }

    public record AiTermsSummary(
            String headline,
            List<String> clauseHighlights,
            String coverageSummary,
            String subscriptionConditions,
            String premiumAndPriceIndex,
            String refundAndInterest,
            String specialNotes
    ) {
    }

    public record AiLightSummary(
            String paymentSummary,
            String coverageSummary,
            String featureSummary
    ) {
    }
}
