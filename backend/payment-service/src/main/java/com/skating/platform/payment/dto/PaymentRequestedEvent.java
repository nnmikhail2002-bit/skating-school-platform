
package com.skating.platform.payment.dto;

import java.math.BigDecimal;

public record PaymentRequestedEvent(
        Long orderId,
        BigDecimal amount,
        String currency,
        String idempotencyKey
) {
}
