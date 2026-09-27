package com.skating.platform.backend.dto.trainingsession.request;

import com.skating.platform.backend.entity.TrainingSessionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTrainingSessionRequest {
    @NotNull(message = "trainer Id invalid")
    private Long trainerId;
    @NotNull(message = "service Id invalid")
    private Long serviceId;
    @NotNull(message = "Time start not correct")
    private LocalDateTime startTime;
    @NotNull(message = "End time not correct")
    private LocalDateTime endTime;
    @NotNull(message = "capacity invalid")
    @Positive
    private Integer capacity;
    @NotNull(message = "status invalid")
    private TrainingSessionStatus status;
}
