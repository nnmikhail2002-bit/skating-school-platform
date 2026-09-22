package com.skating.platform.backend.mapper;

import com.skating.platform.backend.dto.trainerService.response.TrainerServiceResponse;
import com.skating.platform.backend.entity.Trainer;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class TrainerServiceMapper {
    private final SchoolServiceMapper schoolServiceMapper;

    public TrainerServiceMapper(SchoolServiceMapper schoolServiceMapper) {
        this.schoolServiceMapper = schoolServiceMapper;
    }

    public TrainerServiceResponse toResponse(Trainer trainer){
        return new TrainerServiceResponse(
                trainer.getId(),
                trainer.getFirstName(),
                trainer.getLastName(),
                trainer.getServices()
                        .stream()
                        .map(schoolServiceMapper::toResponse)
                        .collect(Collectors.toSet())
        );
    }
}
