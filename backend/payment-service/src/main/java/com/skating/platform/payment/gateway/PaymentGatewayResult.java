
package com.skating.platform.payment.gateway;

public record PaymentGatewayResult(
        boolean successful,
        String providerReference
) {
}
