
package com.skating.platform.payment.gateway;

import com.skating.platform.payment.entity.PaymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MockPaymentGateway implements PaymentGateway {

    private final PaymentStatus finalStatus;

    public MockPaymentGateway(
            @Value("${payment.mock.final-status:SUCCEEDED}")
            PaymentStatus finalStatus
    ) {
        this.finalStatus = finalStatus;
    }

    @Override
    public PaymentGatewayResult processPayment(
            long paymentId,
            BigDecimal amount,
            String currency
    ) {
        return new PaymentGatewayResult(
                PaymentStatus.PENDING,
                "mock-payment-" + paymentId
        );
    }

    @Override
    public PaymentStatus checkPaymentStatus(String providerReference) {
        return finalStatus;
    }
}
