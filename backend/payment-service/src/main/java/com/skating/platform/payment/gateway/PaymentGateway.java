package com.skating.platform.payment.gateway;

import com.skating.platform.payment.entity.PaymentStatus;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentGatewayResult processPayment(
            long paymentId,
            BigDecimal amount,
            String currency
    );

    PaymentStatus checkPaymentStatus(String providerReference);
}