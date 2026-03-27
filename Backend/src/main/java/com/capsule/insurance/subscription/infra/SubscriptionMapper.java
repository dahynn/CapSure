// #Demo Setting
package com.capsule.insurance.subscription.infra;

import com.capsule.insurance.subscription.domain.Subscription;
import com.capsule.insurance.subscription.domain.SubscriptionItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SubscriptionMapper {

    Subscription findSubscriptionAggregateByUserId(@Param("userId") Long userId);

    Subscription findSubscriptionById(@Param("subscriptionId") Long subscriptionId);

    List<SubscriptionItem> findCurrentItemsBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    List<SubscriptionItem> findNextItemsBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    SubscriptionItem findNextItemById(@Param("subscriptionItemId") Long subscriptionItemId);

    boolean existsProductInCurrentOrNext(@Param("subscriptionId") Long subscriptionId,
            @Param("capsuleProductId") Long capsuleProductId);

    void insertNextItem(@Param("subscriptionId") Long subscriptionId,
            @Param("capsuleProductId") Long capsuleProductId);

    void deleteNextItem(@Param("subscriptionItemId") Long subscriptionItemId);

    int countNextItems(@Param("subscriptionId") Long subscriptionId);

    void updateSubscriptionUpdatedAt(@Param("subscriptionId") Long subscriptionId);

    void insertSubscription(Subscription subscription);

    void updateSubscriptionExpectedAmount(@Param("subscriptionId") Long subscriptionId,
            @Param("expectedAmount") java.math.BigDecimal expectedAmount);

    void insertInitialSubscriptionItem(
            @Param("subscriptionId") Long subscriptionId,
            @Param("capsuleProductId") Long capsuleProductId,
            @Param("monthlyPriceSnapshot") java.math.BigDecimal monthlyPriceSnapshot,
            @Param("effectiveStartAt") java.time.Instant effectiveStartAt,
            @Param("effectiveEndAt") java.time.Instant effectiveEndAt);
}
