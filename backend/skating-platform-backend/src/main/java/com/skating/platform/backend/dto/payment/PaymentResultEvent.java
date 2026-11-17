
package com.skating.platform.backend.dto.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResultEvent(
        Long paymentId,
        Long orderId,
        String idempotencyKey,
        BigDecimal amount,
        String currency,
        String status,
        String gatewayReference,
        OffsetDateTime createdAt
) {
}
