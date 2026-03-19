// #Demo Setting
package com.capsule.insurance.subscription.dto;

import java.math.BigDecimal;

public record QuoteResponse(
        String productCode,
        BigDecimal quotedPremium,
        String note
) {
}
