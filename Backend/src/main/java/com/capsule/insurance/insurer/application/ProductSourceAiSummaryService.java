package com.capsule.insurance.insurer.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.insurer.domain.ProductSource;
import com.capsule.insurance.insurer.dto.ProductSourceAiSummaryResponse;
import com.capsule.insurance.insurer.infra.ProductSourceMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductSourceAiSummaryService {

    private static final int MAX_JSON_CHARS = 12000;
    private static final BigDecimal INDEX_SIMILARITY_THRESHOLD = new BigDecimal("3");
    private static final BigDecimal AVERAGE_INDEX = new BigDecimal("100");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private final ProductSourceMapper productSourceMapper;
    private final ObjectMapper objectMapper;
    public ProductSourceAiSummaryService(
            ProductSourceMapper productSourceMapper,
            ObjectMapper objectMapper
    ) {
        this.productSourceMapper = productSourceMapper;
        this.objectMapper = objectMapper;
    }

    public ProductSourceAiSummaryResponse getProductSourceAiSummary(Long productSourceId) {
        ProductSource productSource = productSourceMapper.findById(productSourceId);
        if (productSource == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 product_source를 찾을 수 없습니다.");
        }
        if (!StringUtils.hasText(productSource.getAiSummaryJson())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 상품의 ai_summary_json 데이터가 없습니다.");
        }

        JsonNode summaryJson = parseSummaryJson(productSource.getAiSummaryJson());
        AiJsonSummary fallback = buildFallbackSummary(summaryJson);
        AiJsonSummary aiSummary = generateAiSummary(summaryJson, fallback);
        String premiumSummary = buildPremiumSummary(productSource);

        return new ProductSourceAiSummaryResponse(
                productSource.getProductSourceId(),
                valueOrFallback(productSource.getCompanyName(), "정보 없음"),
                valueOrFallback(productSource.getProductName(), "정보 없음"),
                valueOrFallback(aiSummary.coreCoverage(), fallback.coreCoverage()),
                valueOrFallback(aiSummary.feature(), fallback.feature()),
                premiumSummary
        );
    }

    private JsonNode parseSummaryJson(String aiSummaryJson) {
        try {
            return objectMapper.readTree(aiSummaryJson);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "ai_summary_json 형식이 올바르지 않습니다.");
        }
    }

    private AiJsonSummary generateAiSummary(JsonNode summaryJson, AiJsonSummary fallback) {
        // 외부 모델 호출은 안전한 AI 연동 PoC 경계 밖이다. 현재 상품 요약은 검증 가능한 원본 데이터만 사용한다.
        return fallback;
    }

    private AiJsonSummary buildFallbackSummary(JsonNode summaryJson) {
        JsonNode coverageContent = summaryJson.path("coverage_content");
        JsonNode nonPaymentReasons = summaryJson.path("non_payment_reasons");
        JsonNode premium = summaryJson.path("premium");
        JsonNode coveragePeriod = summaryJson.path("coverage_period");

        String coreCoverage = joinSentences(
                textOrNull(coverageContent, "short_summary"),
                formatCoverageList("주요 반려묘 보장", coverageContent.path("pet_coverages"), 3),
                formatCoverageList("주요 반려인 보장", coverageContent.path("owner_coverages"), 3),
                formatCoverageLimit(coverageContent.path("pet_coverage_limits").path("medical_expense"))
        );

        String feature = joinSentences(
                textOrNull(coveragePeriod, "short_summary"),
                textOrNull(nonPaymentReasons, "short_summary"),
                textOrNull(coveragePeriod, "general_start_rule"),
                joinList("지급 제한", nonPaymentReasons.path("payment_restrictions"), 2)
        );

        String premiumSummary = joinSentences(
                buildPremiumSummaryFromJson(premium)
        );

        return new AiJsonSummary(
                valueOrFallback(coreCoverage, "핵심 보장 정보를 확인할 수 없습니다."),
                valueOrFallback(feature, "상품 특징 정보를 확인할 수 없습니다."),
                valueOrFallback(premiumSummary, "보험료 정보를 확인할 수 없습니다.")
        );
    }

    private String formatCoverageList(String label, JsonNode node, int limit) {
        String joined = joinArray(node, limit);
        if (!StringUtils.hasText(joined)) {
            return null;
        }
        return label + "은 " + joined + " 등이다.";
    }

    private String formatCoverageLimit(JsonNode medicalExpense) {
        String dailyOptions = joinArray(medicalExpense.path("daily_annual_limit_options"), 3);
        String deductible = textOrNull(medicalExpense, "deductible_per_day");
        if (!StringUtils.hasText(dailyOptions) && !StringUtils.hasText(deductible)) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(dailyOptions)) {
            parts.add("의료비 한도는 " + dailyOptions + " 옵션으로 구성된다");
        }
        if (StringUtils.hasText(deductible)) {
            parts.add("자기부담금은 " + deductible + "이다");
        }
        return String.join(", ", parts) + ".";
    }

    private String formatDiscounts(JsonNode discounts) {
        if (discounts.isMissingNode() || discounts.isNull()) {
            return null;
        }

        List<String> items = new ArrayList<>();
        JsonNode animalRegistration = discounts.path("animal_registration_discount");
        JsonNode multiCat = discounts.path("multi_cat_discount");

        if (StringUtils.hasText(textOrNull(animalRegistration, "rate"))) {
            items.add("동물등록 할인 " + textOrNull(animalRegistration, "rate"));
        }
        if (StringUtils.hasText(textOrNull(multiCat, "rate"))) {
            items.add("다묘 할인 " + textOrNull(multiCat, "rate"));
        }

        String combinedRule = textOrNull(discounts, "combined_rule");
        if (items.isEmpty() && !StringUtils.hasText(combinedRule)) {
            return null;
        }

        String base = items.isEmpty() ? null : "할인은 " + String.join(", ", items) + "가 있다.";
        return joinSentences(base, combinedRule);
    }

    private String buildPremiumSummary(ProductSource productSource) {
        BigDecimal maleMonthlyPremium = productSource.getMonthlyPremiumMale();
        BigDecimal femaleMonthlyPremium = productSource.getMonthlyPremiumFemale();
        BigDecimal malePriceIndex = parseDecimal(productSource.getPriceIndexMaleText());
        BigDecimal femalePriceIndex = parseDecimal(productSource.getPriceIndexFemaleText());

        List<String> sentences = new ArrayList<>();

        if (maleMonthlyPremium != null || femaleMonthlyPremium != null) {
            sentences.add(
                    "월 보험료는 남성 "
                            + valueOrUnknown(formatWon(maleMonthlyPremium))
                            + ", 여성 "
                            + valueOrUnknown(formatWon(femaleMonthlyPremium))
                            + "입니다."
            );
        }

        if (malePriceIndex != null && femalePriceIndex != null) {
            BigDecimal difference = malePriceIndex.subtract(femalePriceIndex).abs();
            String comparisonSentence = "보험가격지수는 남성 "
                    + formatDecimal(malePriceIndex)
                    + ", 여성 "
                    + formatDecimal(femalePriceIndex)
                    + "입니다.";

            if (difference.compareTo(INDEX_SIMILARITY_THRESHOLD) <= 0) {
                comparisonSentence += " 차이가 " + formatDecimal(difference) + "p라 사실상 비슷합니다.";
            } else if (malePriceIndex.compareTo(femalePriceIndex) < 0) {
                comparisonSentence += " 지수가 더 낮은 남성 쪽이 상대적으로 유리합니다.";
            } else {
                comparisonSentence += " 지수가 더 낮은 여성 쪽이 상대적으로 유리합니다.";
            }
            sentences.add(comparisonSentence);

            sentences.add(
                    "동일 유형 보험 평균을 100으로 보면 남성은 평균보다 "
                            + describeVsAverage(malePriceIndex)
                            + ", 여성은 "
                            + describeVsAverage(femalePriceIndex)
                            + "입니다."
            );
        } else if (malePriceIndex != null || femalePriceIndex != null) {
            sentences.add(
                    "보험가격지수는 남성 "
                            + valueOrUnknown(formatDecimal(malePriceIndex))
                            + ", 여성 "
                            + valueOrUnknown(formatDecimal(femalePriceIndex))
                            + "로 일부만 확인됩니다."
            );
        }

        if (sentences.isEmpty()) {
            return "보험료 비교에 필요한 월 보험료 또는 보험가격지수 정보가 없습니다.";
        }

        return String.join(" ", sentences);
    }

    private String buildPremiumSummaryFromJson(JsonNode premium) {
        return joinSentences(
                textOrNull(premium, "short_summary"),
                joinList("납입주기", premium.path("payment_cycle"), 4),
                formatDiscounts(premium.path("discounts")),
                formatInterestRates(premium.path("interest_rates"))
        );
    }

    private String formatInterestRates(JsonNode interestRates) {
        if (interestRates.isMissingNode() || interestRates.isNull()) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        addIfPresent(parts, "보장부분 적용이율 " + textOrNull(interestRates, "coverage_part_rate"));
        addIfPresent(parts, "공시이율 " + textOrNull(interestRates, "declared_rate_as_of_2026_01"));
        addIfPresent(parts, "최저보증이율 " + textOrNull(interestRates, "minimum_guaranteed_rate"));
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(", ", parts) + "가 제시되어 있다.";
    }

    private String joinList(String label, JsonNode node, int limit) {
        String joined = joinArray(node, limit);
        if (!StringUtils.hasText(joined)) {
            return null;
        }
        return label + "은 " + joined + " 등이다.";
    }

    private String joinArray(JsonNode node, int limit) {
        if (node == null || !node.isArray()) {
            return null;
        }

        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (StringUtils.hasText(item.asText())) {
                values.add(item.asText().trim());
            }
            if (values.size() == limit) {
                break;
            }
        }
        return values.isEmpty() ? null : String.join(", ", values);
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode child = node.path(fieldName);
        if (child.isMissingNode() || child.isNull()) {
            return null;
        }
        String value = child.asText();
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void addIfPresent(List<String> target, String value) {
        if (StringUtils.hasText(value)) {
            target.add(value);
        }
    }

    private String joinSentences(String... values) {
        List<String> sentences = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                sentences.add(value.trim());
            }
        }
        return sentences.isEmpty() ? null : String.join(" ", sentences);
    }

    private String valueOrFallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private BigDecimal parseDecimal(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(value.replace(",", ""));
        if (!matcher.find()) {
            return null;
        }

        try {
            return new BigDecimal(matcher.group());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String formatWon(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "원";
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String describeVsAverage(BigDecimal indexValue) {
        BigDecimal gap = indexValue.subtract(AVERAGE_INDEX).abs();
        String direction = indexValue.compareTo(AVERAGE_INDEX) >= 0 ? "비쌉니다" : "저렴합니다";
        return formatDecimal(gap) + "% " + direction;
    }

    private String valueOrUnknown(String value) {
        return StringUtils.hasText(value) ? value : "정보 없음";
    }

    public record AiJsonSummary(
            String coreCoverage,
            String feature,
            String premium
    ) {
    }
}
