package com.capsule.insurance.subscription.application;

import com.capsule.insurance.subscription.dto.QuoteRequest;
import com.capsule.insurance.subscription.dto.QuoteResponse;
import java.math.BigDecimal;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.insurer.domain.CapsuleProduct;
import com.capsule.insurance.insurer.infra.InsurerCatalogMapper;
import com.capsule.insurance.subscription.domain.Subscription;
import com.capsule.insurance.subscription.domain.SubscriptionItem;
import com.capsule.insurance.subscription.dto.SubscriptionDetailResponse;
import com.capsule.insurance.subscription.infra.SubscriptionMapper;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionMapper subscriptionMapper;
    private final InsurerCatalogMapper insurerCatalogMapper;

    public QuoteResponse createQuote(QuoteRequest request) {
        // TODO: 실제 요율 계산과 상품 조회 로직을 구현해야 합니다.
        BigDecimal quotedPremium = BigDecimal.valueOf(10000L + (long) request.insuredAge() * 100L);
        return new QuoteResponse(request.productCode(), quotedPremium, "Placeholder quote response");
    }

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
                            capsuleProduct.getCapsuleName(),
                            "캡슐손해보험", 
                            capsuleProduct.getCoverageCategory() != null ? capsuleProduct.getCoverageCategory().name() : "기타"
                    ));
                    
                    coverageDtos.add(new SubscriptionDetailResponse.CoverageDto(
                            capsuleProduct.getCapsuleName() != null ? capsuleProduct.getCapsuleName() : "통합 보장내역",
                            "최대 " + (capsuleProduct.getCoverageAmount() != null ? capsuleProduct.getCoverageAmount() : "0") + 
                                    (capsuleProduct.getCoverageUnit() != null ? capsuleProduct.getCoverageUnit() : "")
                    ));
                }
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy. MM. dd").withZone(ZoneId.systemDefault());
        String startDate = subscription.getCreatedAt() != null ? formatter.format(subscription.getCreatedAt()) : "2023. 10. 15";
        String dateRange = startDate + " ~ 계속";

        return new SubscriptionDetailResponse(
                subscription.getSubscriptionId(),
                "나의 든든한 맞춤 캡슐",
                subscription.getSubscriptionStatus() != null ? subscription.getSubscriptionStatus().name() : "활성화",
                dateRange,
                subscription.getExpectedNextAmount() != null ? subscription.getExpectedNextAmount() : BigDecimal.ZERO,
                productDtos,
                coverageDtos
        );
    }
}
