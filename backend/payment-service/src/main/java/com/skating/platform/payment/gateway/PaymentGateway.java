
package com.skating.platform.payment.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentGatewayResult processPayment(
            long paymentId,
            BigDecimal amount,
            String currency
    );
}
