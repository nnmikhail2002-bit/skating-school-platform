package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.request.TrainerRequest;
import com.skating.platform.backend.dto.request.UpdateTrainerRequest;
import com.skating.platform.backend.dto.response.TrainerResponse;
import com.skating.platform.backend.entity.Trainer;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.TrainerMapper;
import com.skating.platform.backend.repository.TrainerRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TrainerService {
    private final TrainerRepository repository;
    private final TrainerMapper mapper;

    public TrainerService(TrainerRepository repository, TrainerMapper mapper){
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<TrainerResponse> getAllTrainers(){
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public TrainerResponse createTrainer(TrainerRequest request){
        Trainer trainer = mapper.toEntity(request);
        Trainer saved = repository.save(trainer);
        return mapper.toResponse(saved);
    }

    public Trainer getEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Trainer not found")
                );
    }

    public TrainerResponse getTrainerById(Long id){
        Trainer trainer = getEntityById(id);
        return mapper.toResponse(trainer);
    }

    public TrainerResponse updateTrainer(Long id, UpdateTrainerRequest request){

        Trainer existing = getEntityById(id);

        mapper.updateEntity(existing, request);

        Trainer saved = repository.save(existing);

        return mapper.toResponse(saved);
    }

    public void deleteTrainer(Long id){
        getEntityById(id);
        repository.deleteById(id);
    }
}
