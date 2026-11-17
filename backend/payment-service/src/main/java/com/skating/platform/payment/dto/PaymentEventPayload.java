
package com.skating.platform.payment.dto;

import com.skating.platform.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentEventPayload(
        Long paymentId,
        Long orderId,
        String idempotencyKey,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String gatewayReference,
        OffsetDateTime createdAt
) {
}
