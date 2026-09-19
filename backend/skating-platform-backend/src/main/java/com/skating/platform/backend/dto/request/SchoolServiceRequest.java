package com.skating.platform.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SchoolServiceRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be positive")
    private BigDecimal price;
    @NotBlank(message = "Type is  required")
    private String type;
}
