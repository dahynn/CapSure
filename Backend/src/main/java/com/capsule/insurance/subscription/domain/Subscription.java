// #Demo Setting
package com.capsule.insurance.subscription.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Subscription {

    private Long subscriptionId;
    private Long userId;
    private String capsuleName;
    private SubscriptionStatus subscriptionStatus;
    private Integer billingAnchorDay;
    private Instant currentCycleStartAt;
    private Instant currentCycleEndAt;
    private Instant nextBillingAt;
    private BigDecimal expectedNextAmount;
    private Instant pausedAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<SubscriptionItem> currentItems = new ArrayList<>();

    // TODO: 현재는 CURRENT/NEXT를 별도 컬렉션으로 조립한다. 조회 응답에서 NEXT를 어떻게 우선시할지는 정책 확정 후 application 계층에서 정리한다.
    @Builder.Default
    private List<SubscriptionItem> nextItems = new ArrayList<>();

    public boolean containsProductSource(Long productSourceId) {
        if (productSourceId == null) {
            return false;
        }
        return containsProductSource(currentItems, productSourceId)
                || containsProductSource(nextItems, productSourceId);
    }

    public boolean hasReservedChanges() {
        return nextItems != null && !nextItems.isEmpty();
    }

    private static boolean containsProductSource(List<SubscriptionItem> items, Long productSourceId) {
        if (items == null) {
            return false;
        }
        return items.stream()
                .anyMatch(item -> productSourceId.equals(item.getProductSourceId()));
    }
}
