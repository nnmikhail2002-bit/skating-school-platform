
package com.skating.platform.payment.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentGatewayResult processPayment(
            long paymentId,
            BigDecimal amount,
            String currency
    ) {
        return new PaymentGatewayResult(
                true,
                "mock-payment-" + paymentId
        );
    }
}
