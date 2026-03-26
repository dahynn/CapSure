package com.capsule.insurance.subscription.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.insurer.domain.CapsuleProduct;
import com.capsule.insurance.insurer.domain.CoverageCategory;
import com.capsule.insurance.insurer.infra.InsurerCatalogMapper;
import com.capsule.insurance.subscription.domain.Subscription;
import com.capsule.insurance.subscription.domain.SubscriptionItem;
import com.capsule.insurance.subscription.domain.SubscriptionStatus;
import com.capsule.insurance.subscription.dto.SubscriptionDetailResponse;
import com.capsule.insurance.subscription.infra.SubscriptionMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @Mock
    private InsurerCatalogMapper insurerCatalogMapper;

    @Test
    @DisplayName("나의 가입 캡슐 세부 정보를 성공적으로 조회한다")
    void getSubscriptionDetail_Success() {
        // given
        Long userId = 1L;
        Long subId = 1L;

        SubscriptionItem item1 = SubscriptionItem.builder()
                .capsuleProductId(101L)
                .build();
        
        Subscription subscription = Subscription.builder()
                .subscriptionId(subId)
                .userId(userId)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2023, 10, 15, 0, 0))
                .expectedNextAmount(BigDecimal.valueOf(12000))
                .currentItems(List.of(item1))
                .build();

        CapsuleProduct product = CapsuleProduct.builder()
                .capsuleProductId(101L)
                .capsuleName("자전거 배상")
                .coverageCategory(CoverageCategory.LIABILITY)
                .coverageAmount(BigDecimal.valueOf(1000))
                .coverageUnit("만원")
                .build();

        when(subscriptionMapper.findSubscriptionAggregateByUserId(userId)).thenReturn(subscription);
        when(insurerCatalogMapper.findCapsuleProductById(101L)).thenReturn(product);

        // when
        SubscriptionDetailResponse response = subscriptionService.getSubscriptionDetail(userId, subId);

        // then
        assertThat(response.subscriptionId()).isEqualTo(subId);
        assertThat(response.totalPremium()).isEqualTo(BigDecimal.valueOf(12000));
        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).name()).isEqualTo("자전거 배상");
        assertThat(response.coverages()).hasSize(1);
    }

    @Test
    @DisplayName("구독 정보가 없는 사용자가 조회하면 예외가 발생한다")
    void getSubscriptionDetail_ThrowsExceptionWhenNoSubscription() {
        // given
        Long userId = 99L;    // 구독이 없는 사용자
        Long subscriptionId = 1L;

        when(subscriptionMapper.findSubscriptionAggregateByUserId(userId)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> subscriptionService.getSubscriptionDetail(userId, subscriptionId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("해당 캡슐 정보");
    }
}
