
package com.skating.platform.payment.dto;

import com.skating.platform.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        OffsetDateTime createdAt
) {
}
