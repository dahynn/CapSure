package com.capsule.insurance.subscription.domain;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SubscriptionCapsuleSnapshot {

    private Long capsuleSnapshotId;
    private Long subscriptionId;
    private Long userId;
    private String capsuleName;
    private BigDecimal totalPremium;
    private Instant cycleStartedAt;
    private Instant cycleEndedAt;
    private Instant createdAt;
}
