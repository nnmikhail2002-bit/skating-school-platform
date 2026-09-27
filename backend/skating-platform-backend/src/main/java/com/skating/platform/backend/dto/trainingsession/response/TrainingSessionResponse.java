package com.skating.platform.backend.dto.trainingsession.response;

import com.skating.platform.backend.entity.TrainingSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSessionResponse {
    private Long id;
    private Long trainerId;
    private Long serviceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer capacity;
    private TrainingSessionStatus status;
}
