package com.skating.platform.backend.dto.booking.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBookingRequest {
    @NotNull(message = "Training session Id invalid")
    @Positive(message = "Training session Id must be positive")
    private Long trainingSessionId;
}
