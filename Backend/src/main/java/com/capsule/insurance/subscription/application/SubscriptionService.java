// #Demo Setting
package com.capsule.insurance.subscription.application;

import com.capsule.insurance.subscription.dto.QuoteRequest;
import com.capsule.insurance.subscription.dto.QuoteResponse;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService {

    public QuoteResponse createQuote(QuoteRequest request) {
        // TODO: 실제 요율 계산과 상품 조회 로직을 구현해야 합니다.
        BigDecimal quotedPremium = BigDecimal.valueOf(10000L + (long) request.insuredAge() * 100L);
        return new QuoteResponse(request.productCode(), quotedPremium, "Placeholder quote response");
    }
}
