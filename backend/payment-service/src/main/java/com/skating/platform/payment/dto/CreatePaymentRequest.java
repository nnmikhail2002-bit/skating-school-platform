
package com.skating.platform.payment.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreatePaymentRequest(

        @NotNull
        @Positive
        Long orderId,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @NotBlank
        @Pattern(regexp = "[A-Z]{3}")
        String currency,

        @NotBlank
        @Size(max = 128)
        String idempotencyKey
) {
}
