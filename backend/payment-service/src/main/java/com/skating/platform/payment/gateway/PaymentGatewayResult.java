package com.skating.platform.payment.gateway;

import com.skating.platform.payment.entity.PaymentStatus;

public record PaymentGatewayResult(
        PaymentStatus status,
        String providerReference
) {
}