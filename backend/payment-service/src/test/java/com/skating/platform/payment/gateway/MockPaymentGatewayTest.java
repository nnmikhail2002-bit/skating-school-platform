
package com.skating.platform.payment.gateway;

import com.skating.platform.payment.entity.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockPaymentGatewayTest {

    @Test
    void processPayment_shouldReturnPendingResult() {

        PaymentGateway gateway =
                new MockPaymentGateway(PaymentStatus.SUCCEEDED);

        PaymentGatewayResult result = gateway.processPayment(
                42L,
                new BigDecimal("1500.00"),
                "RUB"
        );

        assertEquals(PaymentStatus.PENDING, result.status());
        assertEquals("mock-payment-42", result.providerReference());
    }

    @Test
    void checkPaymentStatus_shouldReturnSucceeded() {

        PaymentGateway gateway =
                new MockPaymentGateway(PaymentStatus.SUCCEEDED);

        PaymentStatus status =
                gateway.checkPaymentStatus("mock-payment-42");

        assertEquals(PaymentStatus.SUCCEEDED, status);
    }

    @Test
    void checkPaymentStatus_shouldReturnFailed() {

        PaymentGateway gateway =
                new MockPaymentGateway(PaymentStatus.FAILED);

        PaymentStatus status =
                gateway.checkPaymentStatus("mock-payment-42");

        assertEquals(PaymentStatus.FAILED, status);
    }
}
