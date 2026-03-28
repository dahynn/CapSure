package com.capsule.insurance.subscription.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.insurer.domain.CapsuleProduct;
import com.capsule.insurance.insurer.infra.InsurerCatalogMapper;
import com.capsule.insurance.insurer.infra.projection.ProductSourceDetailProjection;
import com.capsule.insurance.subscription.domain.Subscription;
import com.capsule.insurance.subscription.domain.SubscriptionCapsuleSnapshot;
import com.capsule.insurance.subscription.domain.SubscriptionItem;
import com.capsule.insurance.subscription.domain.SubscriptionItemStatus;
import com.capsule.insurance.subscription.dto.*;
import com.capsule.insurance.subscription.dto.NextItemsResponse.SubscriptionItemDto;
import com.capsule.insurance.subscription.infra.SubscriptionMapper;
import com.capsule.insurance.subscription.infra.projection.DueSubscriptionBillingProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());
    private static final String DEFAULT_CAPSULE_NAME = "나만의 캡슐";
    private static final Pattern COVERAGE_AMOUNT_TOKEN_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)(억|만|천|원)?");

    private final SubscriptionMapper subscriptionMapper;
    private final InsurerCatalogMapper insurerCatalogMapper;

    /**
     * 최초 캡슐 구독 생성 (생성일 기준 1개월 보장)
     */
    @Transactional
    public Long createInitialSubscription(Long userId, CreateSubscriptionRequest request) {
        List<Long> productSourceIds = request.productSourceIds();
        if (productSourceIds == null || productSourceIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "최소 1개 이상의 보험 상품을 선택해 주세요.");
        }

        Instant now = Instant.now();
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plus(1, ChronoUnit.MONTHS);

        // 1. 구독 마스터 정보 생성
        Subscription subscription = Subscription.builder()
                .userId(userId)
                .capsuleName(resolveCapsuleName(request.capsuleName()))
                .subscriptionStatus(com.capsule.insurance.subscription.domain.SubscriptionStatus.ACTIVE)
                .billingAnchorDay(today.getDayOfMonth())
                .currentCycleStartAt(now)
                .currentCycleEndAt(nextMonth.atStartOfDay(ZoneId.systemDefault()).toInstant())
                .nextBillingAt(nextMonth.atStartOfDay(ZoneId.systemDefault()).toInstant())
                .createdAt(now)
                .updatedAt(now)
                .build();

        subscriptionMapper.insertSubscription(subscription);
        Long subscriptionId = subscription.getSubscriptionId();

        BigDecimal totalExpectedAmount = BigDecimal.ZERO;

        // 2. 선택한 상품들을 구독 아이템(CURRENT)으로 추가
        for (Long productSourceId : productSourceIds) {
            ProductSourceDetailProjection detail = insurerCatalogMapper.findProductSourceDetail(productSourceId, "M");
            if (detail == null)
                continue;

            BigDecimal monthlyPrice = detail.monthlyPrice() != null ? detail.monthlyPrice() : BigDecimal.ZERO;
            totalExpectedAmount = totalExpectedAmount.add(monthlyPrice);

            subscriptionMapper.insertInitialSubscriptionItem(
                    subscriptionId,
                    productSourceId,
                    monthlyPrice,
                    subscription.getCurrentCycleStartAt(),
                    subscription.getCurrentCycleEndAt());
        }

        // 3. 총 보험료 합산치 업데이트
        subscriptionMapper.updateSubscriptionExpectedAmount(subscriptionId, totalExpectedAmount);
        snapshotCapsule(
                subscriptionId,
                userId,
                resolveCapsuleName(request.capsuleName()),
                totalExpectedAmount,
                subscription.getCurrentCycleStartAt(),
                subscription.getCurrentCycleEndAt());

        return subscriptionId;
    }

    @Transactional
    public int processDueRenewals(Instant now) {
        List<DueSubscriptionBillingProjection> dueSubscriptions =
                subscriptionMapper.findDueSubscriptionsForRenewal(now);
        int renewedCount = 0;

        for (DueSubscriptionBillingProjection due : dueSubscriptions) {
            if (!subscriptionMapper.existsActivePaymentMethod(due.userId())) {
                continue;
            }

            Instant cycleStart = due.nextBillingAt() != null ? due.nextBillingAt() : now;
            Instant cycleEnd = LocalDate.ofInstant(cycleStart, ZoneId.systemDefault())
                    .plusMonths(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
            Instant nextBillingAt = cycleEnd;

            List<SubscriptionItem> nextItems = subscriptionMapper.findNextItemsBySubscriptionId(due.subscriptionId());
            boolean hasConfirmedNextItems = nextItems != null
                    && nextItems.stream().anyMatch(item -> item.getItemStatus() == SubscriptionItemStatus.ACTIVE);
            if (hasConfirmedNextItems) {
                subscriptionMapper.deleteCurrentItemsBySubscriptionId(due.subscriptionId());
                subscriptionMapper.promoteNextItemsToCurrent(due.subscriptionId(), cycleStart, cycleEnd);
                // confirmed 반영 이후 NEXT 버전은 모두 비워서 다음 주기 예약 상태를 초기화한다.
                subscriptionMapper.deleteNextItemsBySubscriptionId(due.subscriptionId());
            } else {
                subscriptionMapper.updateCurrentItemsEffectivePeriod(due.subscriptionId(), cycleStart, cycleEnd);
                // 미확정 예약은 주기 종료 시점에 자동 만료 처리한다.
                subscriptionMapper.deleteNextItemsBySubscriptionId(due.subscriptionId());
            }

            BigDecimal expectedAmount = subscriptionMapper.sumCurrentItemsMonthlyPrice(due.subscriptionId());
            if (expectedAmount == null) {
                expectedAmount = BigDecimal.ZERO;
            }

            subscriptionMapper.updateSubscriptionCycle(
                    due.subscriptionId(),
                    cycleStart,
                    cycleEnd,
                    nextBillingAt,
                    expectedAmount);

            snapshotCapsule(
                    due.subscriptionId(),
                    due.userId(),
                    resolveCapsuleName(due.capsuleName()),
                    expectedAmount,
                    cycleStart,
                    cycleEnd);
            renewedCount++;
        }

        return renewedCount;
    }

    @Transactional
    public void registerPaymentMethod(Long userId, RegisterPaymentMethodRequest request) {
        subscriptionMapper.deactivatePaymentMethods(userId);
        subscriptionMapper.insertPaymentMethod(
                userId,
                request.provider().trim(),
                request.methodType().trim(),
                request.maskedNumber().trim());
    }

    @Transactional(readOnly = true)
    public CurrentPaymentMethodResponse getCurrentPaymentMethod(Long userId) {
        return subscriptionMapper.findActivePaymentMethodByUserId(userId);
    }

    public QuoteResponse createQuote(QuoteRequest request) {
        BigDecimal quotedPremium = BigDecimal.valueOf(10000L + (long) request.insuredAge() * 100L);
        return new QuoteResponse(request.productCode(), quotedPremium, "Placeholder quote response");
    }
    // ... (rest of methods remain the same)

    public SubscriptionDetailResponse getSubscriptionDetail(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionMapper.findSubscriptionById(subscriptionId);
        if (subscription == null || !subscription.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 캡슐 정보를 찾을 수 없습니다.");
        }

        List<SubscriptionDetailResponse.ProductDto> productDtos = new ArrayList<>();
        List<SubscriptionDetailResponse.CoverageDto> coverageDtos = new ArrayList<>();

        if (subscription.getCurrentItems() != null) {
            for (SubscriptionItem item : subscription.getCurrentItems()) {
                ProductSourceDetailProjection productSource = insurerCatalogMapper.findProductSourceDetail(
                        item.getCapsuleProductId(),
                        "M");
                if (productSource != null) {
                    productDtos.add(new SubscriptionDetailResponse.ProductDto(
                            productSource.productSourceId(),
                            productSource.productName(),
                            productSource.companyName(),
                            toCategoryLabel(productSource.coverageCategoryCode())));

                    coverageDtos.add(new SubscriptionDetailResponse.CoverageDto(
                            productSource.coverageName() != null ? productSource.coverageName() : "통합 보장내역",
                            resolveCoverageAmountText(productSource.joinAmount(), productSource.payoutAmount())));
                }
            }
        }

        coverageDtos = coverageDtos.stream()
                .sorted(Comparator.comparing(this::parseCoverageAmountForSort).reversed())
                .toList();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy. MM. dd").withZone(ZoneId.systemDefault());
        String startDate = subscription.getCreatedAt() != null ? formatter.format(subscription.getCreatedAt())
                : "2023. 10. 15";
        String dateRange = startDate + " ~ 계속";

        return new SubscriptionDetailResponse(
                subscription.getSubscriptionId(),
                resolveCapsuleName(subscription.getCapsuleName()),
                subscription.getSubscriptionStatus() != null ? subscription.getSubscriptionStatus().name() : "활성화",
                dateRange,
                subscription.getExpectedNextAmount() != null ? subscription.getExpectedNextAmount() : BigDecimal.ZERO,
                productDtos,
                coverageDtos);
    }

    /* ── 익월 예약 보험 조회 ── */
    public NextItemsResponse getNextItems(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionMapper.findSubscriptionById(subscriptionId);
        if (subscription == null || !subscription.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 구독 정보를 찾을 수 없습니다.");
        }

        List<SubscriptionItem> currentItems = subscriptionMapper.findCurrentItemsBySubscriptionId(subscriptionId);
        List<SubscriptionItem> nextItems = subscriptionMapper.findNextItemsBySubscriptionId(subscriptionId);

        return new NextItemsResponse(
                subscriptionId,
                subscription.getBillingAnchorDay(),
                subscription.getNextBillingAt() != null ? DATE_FORMATTER.format(subscription.getNextBillingAt()) : null,
                toItemDtos(currentItems),
                toItemDtos(nextItems));
    }

    /* ── 익월 보험 예약 추가 ── */
    public ReservedItemResponse reserveNextItem(Long userId, Long subscriptionId, Long capsuleProductId) {
        Subscription subscription = subscriptionMapper.findSubscriptionById(subscriptionId);
        if (subscription == null || !subscription.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 구독 정보를 찾을 수 없습니다.");
        }

        CapsuleProduct product = insurerCatalogMapper.findCapsuleProductById(capsuleProductId);
        if (product == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 캡슐 상품을 찾을 수 없습니다.");
        }

        if (subscriptionMapper.existsProductInCurrentOrNext(subscriptionId, capsuleProductId)) {
            throw new BusinessException(ErrorCode.DUPLICATED_RESOURCE, "이미 예약된 상품입니다.");
        }

        subscriptionMapper.insertNextItem(subscriptionId, capsuleProductId);
        subscriptionMapper.updateSubscriptionUpdatedAt(subscriptionId);

        // 방금 삽입된 아이템 조회 (최신순)
        List<SubscriptionItem> nextItems = subscriptionMapper.findNextItemsBySubscriptionId(subscriptionId);
        SubscriptionItem inserted = nextItems.isEmpty() ? null : nextItems.get(nextItems.size() - 1);

        return new ReservedItemResponse(
                inserted != null ? inserted.getSubscriptionItemId() : null,
                product.getCapsuleProductId(),
                product.getProductName(),
                "캡슐손해보험",
                product.getMonthlyPriceMale() != null ? product.getMonthlyPriceMale().intValue() : 0,
                SubscriptionItemStatus.RESERVED_ADD.name());
    }

    /* ── 익월 보험 예약 취소 ── */
    public void cancelNextItem(Long userId, Long subscriptionId, Long subscriptionItemId) {
        Subscription subscription = subscriptionMapper.findSubscriptionById(subscriptionId);
        if (subscription == null || !subscription.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 구독 정보를 찾을 수 없습니다.");
        }

        SubscriptionItem item = subscriptionMapper.findNextItemById(subscriptionItemId);
        if (item == null || !item.getSubscriptionId().equals(subscriptionId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 구독 아이템을 찾을 수 없습니다.");
        }

        if (item.getItemStatus() != SubscriptionItemStatus.RESERVED_ADD) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "취소할 수 없는 상태의 아이템입니다.");
        }

        subscriptionMapper.deleteNextItem(subscriptionItemId);
        subscriptionMapper.updateSubscriptionUpdatedAt(subscriptionId);
    }

    /* ── 익월 캡슐 변경 확정 ── */
    public ConfirmNextResponse confirmNext(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionMapper.findSubscriptionById(subscriptionId);
        if (subscription == null || !subscription.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 구독 정보를 찾을 수 없습니다.");
        }

        int count = subscriptionMapper.countNextItems(subscriptionId);
        if (count == 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "확정할 예약 아이템이 없습니다.");
        }

        // RESERVED_ADD -> ACTIVE 로 승격해 갱신 배치가 반영할 수 있도록 확정 상태를 명시한다.
        subscriptionMapper.updateNextItemsStatus(
                subscriptionId,
                SubscriptionItemStatus.RESERVED_ADD.name(),
                SubscriptionItemStatus.ACTIVE.name());
        subscriptionMapper.updateSubscriptionUpdatedAt(subscriptionId);

        return new ConfirmNextResponse(
                subscriptionId,
                subscription.getNextBillingAt() != null ? DATE_FORMATTER.format(subscription.getNextBillingAt()) : null,
                count);
    }

    /* ── 내부 헬퍼 ── */
    private String toCategoryLabel(String coverageCategoryCode) {
        if (coverageCategoryCode == null) {
            return "기타";
        }
        return switch (coverageCategoryCode) {
            case "DEATH" -> "사망";
            case "CANCER" -> "암";
            case "BRAIN_HEART" -> "뇌/심장";
            case "ACTUAL_LOSS" -> "실손";
            case "SURGERY" -> "수술";
            case "ACCIDENT" -> "상해";
            case "LIABILITY" -> "일상배상책임";
            default -> "기타";
        };
    }

    private String resolveCoverageAmountText(String joinAmount, String payoutAmount) {
        if (joinAmount != null && !joinAmount.isBlank()) {
            return joinAmount;
        }
        if (payoutAmount != null && !payoutAmount.isBlank()) {
            return payoutAmount;
        }
        return "-";
    }

    private BigDecimal parseCoverageAmountForSort(SubscriptionDetailResponse.CoverageDto coverage) {
        if (coverage == null || coverage.amount() == null || coverage.amount().isBlank() || "-".equals(coverage.amount())) {
            return BigDecimal.valueOf(-1L);
        }

        String normalized = coverage.amount().replace(",", "").replaceAll("\\s+", "");
        Matcher matcher = COVERAGE_AMOUNT_TOKEN_PATTERN.matcher(normalized);
        BigDecimal totalAmount = BigDecimal.ZERO;
        boolean matched = false;

        while (matcher.find()) {
            matched = true;
            BigDecimal value;
            try {
                value = new BigDecimal(matcher.group(1));
            } catch (NumberFormatException e) {
                continue;
            }

            String unit = matcher.group(2);
            BigDecimal multiplier = switch (unit == null ? "" : unit) {
                case "억" -> BigDecimal.valueOf(100_000_000L);
                case "만" -> BigDecimal.valueOf(10_000L);
                case "천" -> BigDecimal.valueOf(1_000L);
                default -> BigDecimal.ONE;
            };

            totalAmount = totalAmount.add(value.multiply(multiplier));
        }

        if (!matched) {
            return BigDecimal.valueOf(-1L);
        }

        return totalAmount;
    }

    private List<SubscriptionItemDto> toItemDtos(List<SubscriptionItem> items) {
        if (items == null)
            return List.of();
        return items.stream()
                .map(item -> {
                    CapsuleProduct p = insurerCatalogMapper.findCapsuleProductById(item.getCapsuleProductId());
                    return new SubscriptionItemDto(
                            item.getSubscriptionItemId(),
                            item.getCapsuleProductId(),
                            p != null ? p.getProductName() : "알 수 없음",
                            "캡슐손해보험",
                            item.getMonthlyPriceSnapshot() != null ? item.getMonthlyPriceSnapshot().intValue() : 0,
                            item.getItemStatus() != null ? item.getItemStatus().name() : "");
                })
                .collect(Collectors.toList());
    }

    private String resolveCapsuleName(String capsuleName) {
        if (capsuleName == null || capsuleName.isBlank()) {
            return DEFAULT_CAPSULE_NAME;
        }
        return capsuleName.trim();
    }

    private void snapshotCapsule(
            Long subscriptionId,
            Long userId,
            String capsuleName,
            BigDecimal totalPremium,
            Instant cycleStartedAt,
            Instant cycleEndedAt
    ) {
        SubscriptionCapsuleSnapshot snapshot = SubscriptionCapsuleSnapshot.builder()
                .subscriptionId(subscriptionId)
                .userId(userId)
                .capsuleName(capsuleName)
                .totalPremium(totalPremium != null ? totalPremium : BigDecimal.ZERO)
                .cycleStartedAt(cycleStartedAt)
                .cycleEndedAt(cycleEndedAt)
                .build();
        subscriptionMapper.insertCapsuleSnapshot(snapshot);
        subscriptionMapper.insertCapsuleSnapshotItemsFromCurrent(snapshot.getCapsuleSnapshotId(), subscriptionId);
    }
}
