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

    List<SubscriptionItem> findCurrentItemsBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    List<SubscriptionItem> findNextItemsBySubscriptionId(@Param("subscriptionId") Long subscriptionId);
}
