package com.skating.platform.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrainerRequest {
    @NotBlank(message = "First name is required")
    @Size(
            min = 2,
            max = 20,
            message = "First name must be between 2 and 20 characters"
    )
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(
            min = 2,
            max = 50,
            message = "Last name must be between 2 and 50 characters"
    )
    private String lastName;

    @NotBlank(message = "Phone is required")
    @Pattern(
            regexp = "^\\+?[0-9]{10,15}$",
            message = "Invalid phone number"
    )
    private String phone;

    @NotNull(message = "Experience years is required")
    @PositiveOrZero(message = "Experience years cannot be negative")
    private Integer experienceYears;
}
