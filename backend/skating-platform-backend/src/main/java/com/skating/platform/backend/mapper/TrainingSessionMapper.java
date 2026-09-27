package com.skating.platform.backend.mapper;
import com.skating.platform.backend.dto.trainingsession.response.TrainingSessionResponse;
import com.skating.platform.backend.entity.TrainingSession;
import org.springframework.stereotype.Component;


@Component
public class TrainingSessionMapper {

    public TrainingSessionResponse toResponse(TrainingSession session){
        return new TrainingSessionResponse(
                session.getId(),
                session.getTrainer().getId(),
                session.getService().getId(),
                session.getStartTime(),
                session.getEndTime(),
                session.getCapacity(),
                session.getStatus()
        );
    }
}
