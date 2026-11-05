
package com.skating.platform.payment.gateway;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockPaymentGatewayTest {

    @Test
    void processPayment_shouldReturnSuccessfulResult() {
        PaymentGateway gateway = new MockPaymentGateway();

        PaymentGatewayResult result = gateway.processPayment(
                42L,
                new BigDecimal("1500.00"),
                "RUB"
        );

        assertTrue(result.successful());
        assertEquals("mock-payment-42", result.providerReference());
    }
}
