package com.capsule.insurance.subscription.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.insurer.domain.CapsuleProduct;
import com.capsule.insurance.insurer.dto.ProductDetailResponse;
import com.capsule.insurance.insurer.infra.InsurerCatalogMapper;
import com.capsule.insurance.subscription.domain.Subscription;
import com.capsule.insurance.subscription.domain.SubscriptionItem;
import com.capsule.insurance.subscription.domain.SubscriptionItemStatus;
import com.capsule.insurance.subscription.dto.*;
import com.capsule.insurance.subscription.dto.NextItemsResponse.SubscriptionItemDto;
import com.capsule.insurance.subscription.infra.SubscriptionMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    private final SubscriptionMapper subscriptionMapper;
    private final InsurerCatalogMapper insurerCatalogMapper;

    /**
     * 최초 캡슐 구독 생성 (생성일 기준 1개월 보장)
     */
    @Transactional
    public Long createInitialSubscription(Long userId, CreateSubscriptionRequest request) {
        // 이미 활성 구독이 있는지 확인 (사용자 당 1개 캡슐 전제)
        Subscription existing = subscriptionMapper.findSubscriptionAggregateByUserId(userId);
        if (existing != null && "ACTIVE".equals(existing.getSubscriptionStatus().name())) {
            throw new BusinessException(ErrorCode.DUPLICATED_RESOURCE, "이미 활성화된 보험 캡슐이 존재합니다.");
        }

        Instant now = Instant.now();
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plus(1, ChronoUnit.MONTHS);

        // 1. 구독 마스터 정보 생성
        Subscription subscription = Subscription.builder()
                .userId(userId)
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
        for (Long productSourceId : request.productSourceIds()) {
            ProductDetailResponse detail = insurerCatalogMapper.findProductSourceDetail(productSourceId, "M"); // 기본 'M'
                                                                                                               // 세팅, 추후
                                                                                                               // 사용자 성별
                                                                                                               // 연동
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

        return subscriptionId;
    }

    public QuoteResponse createQuote(QuoteRequest request) {
        BigDecimal quotedPremium = BigDecimal.valueOf(10000L + (long) request.insuredAge() * 100L);
        return new QuoteResponse(request.productCode(), quotedPremium, "Placeholder quote response");
    }
    // ... (rest of methods remain the same)

    public SubscriptionDetailResponse getSubscriptionDetail(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionMapper.findSubscriptionAggregateByUserId(userId);
        if (subscription == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 캡슐 정보를 찾을 수 없습니다.");
        }

        List<SubscriptionDetailResponse.ProductDto> productDtos = new ArrayList<>();
        List<SubscriptionDetailResponse.CoverageDto> coverageDtos = new ArrayList<>();

        if (subscription.getCurrentItems() != null) {
            for (SubscriptionItem item : subscription.getCurrentItems()) {
                CapsuleProduct capsuleProduct = insurerCatalogMapper.findCapsuleProductById(item.getCapsuleProductId());
                if (capsuleProduct != null) {
                    productDtos.add(new SubscriptionDetailResponse.ProductDto(
                            capsuleProduct.getCapsuleProductId(),
                            capsuleProduct.getProductName(),
                            "캡슐손해보험",
                            capsuleProduct.getCoverageCategory() != null ? capsuleProduct.getCoverageCategory().name()
                                    : "기타"));

                    coverageDtos.add(new SubscriptionDetailResponse.CoverageDto(
                            capsuleProduct.getProductName() != null ? capsuleProduct.getProductName() : "통합 보장내역",
                            "최대 " + (capsuleProduct.getCoverageAmount() != null ? capsuleProduct.getCoverageAmount()
                                    : "0")));
                }
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy. MM. dd").withZone(ZoneId.systemDefault());
        String startDate = subscription.getCreatedAt() != null ? formatter.format(subscription.getCreatedAt())
                : "2023. 10. 15";
        String dateRange = startDate + " ~ 계속";

        return new SubscriptionDetailResponse(
                subscription.getSubscriptionId(),
                "나의 든든한 맞춤 캡슐",
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

        subscriptionMapper.updateSubscriptionUpdatedAt(subscriptionId);

        return new ConfirmNextResponse(
                subscriptionId,
                subscription.getNextBillingAt() != null ? DATE_FORMATTER.format(subscription.getNextBillingAt()) : null,
                count);
    }

    /* ── 내부 헬퍼 ── */
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
}
