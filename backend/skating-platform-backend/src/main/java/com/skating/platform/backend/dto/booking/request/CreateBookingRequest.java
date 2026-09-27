package com.skating.platform.backend.dto.booking.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateBookingRequest {
    @NotNull(message = "Student Id invalid")
    private Long studentId;
    @NotNull(message = "Training session Id invalid")
    private Long trainingSessionId;
}
