package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.trainer.request.CreateTrainerRequest;
import com.skating.platform.backend.dto.trainer.request.UpdateTrainerRequest;
import com.skating.platform.backend.dto.trainer.response.TrainerResponse;
import com.skating.platform.backend.dto.trainerService.response.TrainerServiceResponse;
import com.skating.platform.backend.entity.SchoolService;
import com.skating.platform.backend.entity.Trainer;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.TrainerMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.skating.platform.backend.mapper.TrainerServiceMapper;
import com.skating.platform.backend.repository.TrainerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class TrainerService {
    private final TrainerServiceMapper trainerServiceMapper;
    private final TrainerRepository repository;
    private final TrainerMapper mapper;
    private final SchoolServiceService schoolServiceService;

    public TrainerService(TrainerRepository repository, TrainerMapper mapper, SchoolServiceService schoolServiceService, TrainerServiceMapper trainerServiceMapper){
        this.repository = repository;
        this.mapper = mapper;
        this.schoolServiceService = schoolServiceService;
        this.trainerServiceMapper = trainerServiceMapper;
    }

    public Page<TrainerResponse> getAllTrainers(Pageable pageable){
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    public TrainerResponse createTrainer(CreateTrainerRequest request){
        if (repository.existsByPhone(request.getPhone())){
            throw new ConflictException(
                    "Trainer with this phone already exists"
            );
        }
        Trainer trainer = mapper.toEntity(request);
        Trainer saved = repository.save(trainer);
        return mapper.toResponse(saved);
    }

    Trainer getEntityById(Long id) {
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

        if(repository.existsByPhone(request.getPhone())
                && !existing.getPhone().equals(request.getPhone())){

            throw new ConflictException(
                    "Trainer with this phone already exists"
            );
        }

        mapper.updateEntity(existing, request);

        Trainer saved = repository.save(existing);

        return mapper.toResponse(saved);
    }

    @Transactional
    public void deleteTrainer(Long trainerId){
        Trainer trainer = getEntityById(trainerId);
        trainer.getServices().clear();
        repository.delete(trainer);

    }

    @Transactional
    public void addServiceToTrainer(Long trainerId, Long serviceId){
        Trainer trainer = getEntityById(trainerId);
        SchoolService service = schoolServiceService.getEntityById(serviceId);
        trainer.getServices().add(service);
        repository.save(trainer);
    }

    @Transactional
    public Page<TrainerServiceResponse> getAllTrainersServices(Pageable pageable){
        return repository.findAll(pageable)
                .map(trainerServiceMapper::toResponse);
    }

    @Transactional
    public Page<TrainerServiceResponse> getAllTrainersWithServices(Pageable pageable){
        return repository.findDistinctByServicesIsNotEmpty(pageable)
                .map(trainerServiceMapper::toResponse);
    }

    @Transactional
    public TrainerServiceResponse getTrainerServiceById(Long id){
        Trainer trainer = getEntityById(id);
        return trainerServiceMapper.toResponse(trainer);
    }

    @Transactional
    public void deleteTrainerService (Long trainerId, Long serviceId){
        Trainer trainer = getEntityById(trainerId);
        SchoolService service = schoolServiceService.getEntityById(serviceId);
        trainer.getServices().remove(service);
        repository.save(trainer);
    }
}
