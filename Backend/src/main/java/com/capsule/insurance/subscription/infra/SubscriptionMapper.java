// #Demo Setting
package com.capsule.insurance.subscription.infra;

import com.capsule.insurance.subscription.domain.Subscription;
import com.capsule.insurance.subscription.domain.SubscriptionCapsuleSnapshot;
import com.capsule.insurance.subscription.domain.SubscriptionItem;
import com.capsule.insurance.subscription.dto.CurrentPaymentMethodResponse;
import com.capsule.insurance.subscription.infra.projection.DueSubscriptionBillingProjection;
import com.capsule.insurance.subscription.infra.projection.RecentSubscriptionHomeProjection;
import com.capsule.insurance.subscription.infra.projection.RenewalSoonInsuranceProjection;
import com.capsule.insurance.subscription.infra.projection.SnapshotCategoryCodeProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SubscriptionMapper {

    Subscription findSubscriptionAggregateByUserId(@Param("userId") Long userId);

    List<Subscription> findActiveSubscriptionsByUserId(@Param("userId") Long userId);

    Subscription findSubscriptionById(@Param("subscriptionId") Long subscriptionId);

    List<SubscriptionItem> findCurrentItemsBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    List<SubscriptionItem> findNextItemsBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    SubscriptionItem findNextItemById(@Param("subscriptionItemId") Long subscriptionItemId);

    boolean existsProductInCurrentOrNext(@Param("subscriptionId") Long subscriptionId,
            @Param("productSourceId") Long productSourceId);

    void insertNextItem(@Param("subscriptionId") Long subscriptionId,
            @Param("productSourceId") Long productSourceId);

    void deleteNextItem(@Param("subscriptionItemId") Long subscriptionItemId);

    int countNextItems(@Param("subscriptionId") Long subscriptionId);

    void updateSubscriptionUpdatedAt(@Param("subscriptionId") Long subscriptionId);

    void insertSubscription(Subscription subscription);

    void updateSubscriptionExpectedAmount(@Param("subscriptionId") Long subscriptionId,
            @Param("expectedAmount") java.math.BigDecimal expectedAmount);

    void insertInitialSubscriptionItem(
            @Param("subscriptionId") Long subscriptionId,
            @Param("productSourceId") Long productSourceId,
            @Param("monthlyPriceSnapshot") java.math.BigDecimal monthlyPriceSnapshot,
            @Param("effectiveStartAt") java.time.Instant effectiveStartAt,
            @Param("effectiveEndAt") java.time.Instant effectiveEndAt);

    List<RecentSubscriptionHomeProjection> findRecentSubscriptionsForHome(@Param("userId") Long userId);

    List<SnapshotCategoryCodeProjection> findCategoryCodesBySnapshotIds(
            @Param("capsuleSnapshotIds") List<Long> capsuleSnapshotIds);

    List<RenewalSoonInsuranceProjection> findRenewalSoonInsurancesForHome(@Param("userId") Long userId);

    List<DueSubscriptionBillingProjection> findDueSubscriptionsForRenewal(@Param("now") Instant now);

    boolean existsActivePaymentMethod(@Param("userId") Long userId);

    CurrentPaymentMethodResponse findActivePaymentMethodByUserId(@Param("userId") Long userId);

    void deleteCurrentItemsBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    void promoteNextItemsToCurrent(
            @Param("subscriptionId") Long subscriptionId,
            @Param("effectiveStartAt") Instant effectiveStartAt,
            @Param("effectiveEndAt") Instant effectiveEndAt);

    void updateNextItemsStatus(
            @Param("subscriptionId") Long subscriptionId,
            @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus);

    void deleteNextItemsBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    void updateCurrentItemsEffectivePeriod(
            @Param("subscriptionId") Long subscriptionId,
            @Param("effectiveStartAt") Instant effectiveStartAt,
            @Param("effectiveEndAt") Instant effectiveEndAt);

    BigDecimal sumCurrentItemsMonthlyPrice(@Param("subscriptionId") Long subscriptionId);

    void updateSubscriptionCycle(
            @Param("subscriptionId") Long subscriptionId,
            @Param("currentCycleStartAt") Instant currentCycleStartAt,
            @Param("currentCycleEndAt") Instant currentCycleEndAt,
            @Param("nextBillingAt") Instant nextBillingAt,
            @Param("expectedNextAmount") BigDecimal expectedNextAmount);

    void insertCapsuleSnapshot(SubscriptionCapsuleSnapshot snapshot);

    void insertCapsuleSnapshotItemsFromCurrent(
            @Param("capsuleSnapshotId") Long capsuleSnapshotId,
            @Param("subscriptionId") Long subscriptionId);

    void deactivatePaymentMethods(@Param("userId") Long userId);

    boolean existsUserById(@Param("userId") Long userId);

    void insertPaymentMethod(
            @Param("userId") Long userId,
            @Param("provider") String provider,
            @Param("methodType") String methodType,
            @Param("maskedNumber") String maskedNumber);
}
