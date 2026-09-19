package com.skating.platform.backend.mapper;

import com.skating.platform.backend.dto.request.TrainerRequest;
import com.skating.platform.backend.dto.request.UpdateTrainerRequest;
import com.skating.platform.backend.dto.response.TrainerResponse;
import com.skating.platform.backend.entity.Trainer;
import org.springframework.stereotype.Component;

@Component
public class TrainerMapper {
    public TrainerResponse toResponse(Trainer trainer){
        return new TrainerResponse(
                trainer.getId(),
                trainer.getFirstName(),
                trainer.getLastName(),
                trainer.getPhone(),
                trainer.getExperienceYears(),
                trainer.getActive()
        );
    }

    public Trainer toEntity(TrainerRequest request){
        Trainer trainer = new Trainer();
        trainer.setFirstName(request.getFirstName());
        trainer.setLastName(request.getLastName());
        trainer.setPhone(request.getPhone());
        trainer.setExperienceYears(request.getExperienceYears());
        return trainer;
    }

    public void updateEntity(Trainer trainer, UpdateTrainerRequest request){
        trainer.setFirstName(request.getFirstName());
        trainer.setLastName(request.getLastName());
        trainer.setPhone(request.getPhone());
        trainer.setExperienceYears(request.getExperienceYears());
    }
}
