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
                .createdAt(java.time.Instant.parse("2023-10-15T00:00:00Z"))
                .expectedNextAmount(BigDecimal.valueOf(12000))
                .currentItems(List.of(item1))
                .build();

        CapsuleProduct product = CapsuleProduct.builder()
                .capsuleProductId(101L)
                .productName("자전거 배상")
                .coverageCategory(CoverageCategory.LIABILITY)
                .coverageAmount(BigDecimal.valueOf(1000))
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
        Long userId = 99L;
        Long subscriptionId = 1L;

        when(subscriptionMapper.findSubscriptionAggregateByUserId(userId)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> subscriptionService.getSubscriptionDetail(userId, subscriptionId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("해당 캡슐 정보");
    }

    // ============================================
    // getNextItems Tests
    // ============================================

    @Test
    @DisplayName("익월 예약 보험 조회 성공")
    void getNextItems_Success() {
        Long userId = 1L;
        Long subId = 1L;

        Subscription sub = Subscription.builder()
                .subscriptionId(subId)
                .userId(userId)
                .billingAnchorDay(25)
                .nextBillingAt(java.time.Instant.parse("2026-04-25T00:00:00Z"))
                .build();

        SubscriptionItem currentItem = SubscriptionItem.builder()
                .subscriptionItemId(10L)
                .capsuleProductId(101L)
                .monthlyPriceSnapshot(BigDecimal.valueOf(10000))
                .build();

        SubscriptionItem nextItem = SubscriptionItem.builder()
                .subscriptionItemId(20L)
                .capsuleProductId(202L)
                .monthlyPriceSnapshot(BigDecimal.valueOf(5000))
                .build();

        CapsuleProduct cp1 = CapsuleProduct.builder().capsuleProductId(101L).capsuleName("ProductA").build();
        CapsuleProduct cp2 = CapsuleProduct.builder().capsuleProductId(202L).capsuleName("ProductB").build();

        when(subscriptionMapper.findSubscriptionById(subId)).thenReturn(sub);
        when(subscriptionMapper.findCurrentItemsBySubscriptionId(subId)).thenReturn(List.of(currentItem));
        when(subscriptionMapper.findNextItemsBySubscriptionId(subId)).thenReturn(List.of(nextItem));
        when(insurerCatalogMapper.findCapsuleProductById(101L)).thenReturn(cp1);
        when(insurerCatalogMapper.findCapsuleProductById(202L)).thenReturn(cp2);

        var result = subscriptionService.getNextItems(userId, subId);

        assertThat(result.subscriptionId()).isEqualTo(subId);
        assertThat(result.billingAnchorDay()).isEqualTo(25);
        assertThat(result.nextBillingAt()).isEqualTo("2026-04-25");
        assertThat(result.currentItems()).hasSize(1);
        assertThat(result.currentItems().get(0).productName()).isEqualTo("ProductA");
        assertThat(result.nextItems()).hasSize(1);
        assertThat(result.nextItems().get(0).productName()).isEqualTo("ProductB");
    }

    @Test
    @DisplayName("익월 예약 보험 조회 실패 - 타인 소유 또는 존재하지 않는 구독")
    void getNextItems_ThrowsException_NotFoundOrNotOwner() {
        Long userId = 1L;
        Long subId = 1L;
        Subscription sub = Subscription.builder().subscriptionId(subId).userId(2L).build(); // Different owner

        when(subscriptionMapper.findSubscriptionById(subId)).thenReturn(sub);

        assertThatThrownBy(() -> subscriptionService.getNextItems(userId, subId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("해당 구독 정보를 찾을 수 없습니다");
    }

    // ============================================
    // reserveNextItem Tests
    // ============================================

    @Test
    @DisplayName("익월 보험 예약 추가 성공")
    void reserveNextItem_Success() {
        Long userId = 1L;
        Long subId = 1L;
        Long cpId = 202L;

        Subscription sub = Subscription.builder().subscriptionId(subId).userId(userId).build();
        CapsuleProduct cp = CapsuleProduct.builder().capsuleProductId(cpId).capsuleName("ProductB").monthlyPrice(BigDecimal.valueOf(5000)).build();
        SubscriptionItem nextItem = SubscriptionItem.builder().subscriptionItemId(99L).capsuleProductId(cpId).build();

        when(subscriptionMapper.findSubscriptionById(subId)).thenReturn(sub);
        when(insurerCatalogMapper.findCapsuleProductById(cpId)).thenReturn(cp);
        when(subscriptionMapper.existsProductInCurrentOrNext(subId, cpId)).thenReturn(false);
        when(subscriptionMapper.findNextItemsBySubscriptionId(subId)).thenReturn(List.of(nextItem));

        var result = subscriptionService.reserveNextItem(userId, subId, cpId);

        assertThat(result.capsuleProductId()).isEqualTo(cpId);
        assertThat(result.productName()).isEqualTo("ProductB");
        assertThat(result.monthlyPrice()).isEqualTo(5000);
    }

    @Test
    @DisplayName("익월 보험 예약 추가 실패 - 이미 예약된 상품")
    void reserveNextItem_ThrowsException_Duplicated() {
        Long userId = 1L;
        Long subId = 1L;
        Long cpId = 202L;

        Subscription sub = Subscription.builder().subscriptionId(subId).userId(userId).build();
        CapsuleProduct cp = CapsuleProduct.builder().capsuleProductId(cpId).build();

        when(subscriptionMapper.findSubscriptionById(subId)).thenReturn(sub);
        when(insurerCatalogMapper.findCapsuleProductById(cpId)).thenReturn(cp);
        when(subscriptionMapper.existsProductInCurrentOrNext(subId, cpId)).thenReturn(true);

        assertThatThrownBy(() -> subscriptionService.reserveNextItem(userId, subId, cpId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 예약된 상품입니다");
    }

    // ============================================
    // cancelNextItem Tests
    // ============================================

    @Test
    @DisplayName("익월 보험 예약 취소 성공")
    void cancelNextItem_Success() {
        Long userId = 1L;
        Long subId = 1L;
        Long itemId = 99L;

        Subscription sub = Subscription.builder().subscriptionId(subId).userId(userId).build();
        SubscriptionItem item = SubscriptionItem.builder()
                .subscriptionItemId(itemId)
                .subscriptionId(subId)
                .itemStatus(com.capsule.insurance.subscription.domain.SubscriptionItemStatus.RESERVED_ADD)
                .build();

        when(subscriptionMapper.findSubscriptionById(subId)).thenReturn(sub);
        when(subscriptionMapper.findNextItemById(itemId)).thenReturn(item);

        subscriptionService.cancelNextItem(userId, subId, itemId);
        // Will throw exception if failed. Verify delete was called.
        org.mockito.Mockito.verify(subscriptionMapper).deleteNextItem(itemId);
    }

    @Test
    @DisplayName("익월 보험 예약 취소 실패 - 올바르지 않은 상태")
    void cancelNextItem_ThrowsException_InvalidStatus() {
        Long userId = 1L;
        Long subId = 1L;
        Long itemId = 99L;

        Subscription sub = Subscription.builder().subscriptionId(subId).userId(userId).build();
        SubscriptionItem item = SubscriptionItem.builder()
                .subscriptionItemId(itemId)
                .subscriptionId(subId)
                // RESERVED_ADD가 아님 (예: ACTIVE)
                .itemStatus(com.capsule.insurance.subscription.domain.SubscriptionItemStatus.ACTIVE)
                .build();

        when(subscriptionMapper.findSubscriptionById(subId)).thenReturn(sub);
        when(subscriptionMapper.findNextItemById(itemId)).thenReturn(item);

        assertThatThrownBy(() -> subscriptionService.cancelNextItem(userId, subId, itemId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("취소할 수 없는 상태");
    }

    // ============================================
    // confirmNext Tests
    // ============================================

    @Test
    @DisplayName("익월 캡슐 변경 확정 성공")
    void confirmNext_Success() {
        Long userId = 1L;
        Long subId = 1L;

        Subscription sub = Subscription.builder()
                .subscriptionId(subId)
                .userId(userId)
                .nextBillingAt(java.time.Instant.parse("2026-04-25T00:00:00Z"))
                .build();

        when(subscriptionMapper.findSubscriptionById(subId)).thenReturn(sub);
        when(subscriptionMapper.countNextItems(subId)).thenReturn(2); // 2개 예약됨

        var result = subscriptionService.confirmNext(userId, subId);

        assertThat(result.subscriptionId()).isEqualTo(subId);
        assertThat(result.nextBillingAt()).isEqualTo("2026-04-25");
        assertThat(result.confirmedItemCount()).isEqualTo(2);
        
        org.mockito.Mockito.verify(subscriptionMapper).updateSubscriptionUpdatedAt(subId);
    }

    @Test
    @DisplayName("익월 캡슐 변경 확정 실패 - 예약 아이템 없음")
    void confirmNext_ThrowsException_EmptyItems() {
        Long userId = 1L;
        Long subId = 1L;

        Subscription sub = Subscription.builder().subscriptionId(subId).userId(userId).build();

        when(subscriptionMapper.findSubscriptionById(subId)).thenReturn(sub);
        when(subscriptionMapper.countNextItems(subId)).thenReturn(0);

        assertThatThrownBy(() -> subscriptionService.confirmNext(userId, subId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("확정할 예약 아이템이 없습니다");
    }
}
