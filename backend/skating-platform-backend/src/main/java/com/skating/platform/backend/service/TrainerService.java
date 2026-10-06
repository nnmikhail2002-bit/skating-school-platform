package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.trainer.request.CreateTrainerRequest;
import com.skating.platform.backend.dto.trainer.request.UpdateTrainerRequest;
import com.skating.platform.backend.dto.trainer.response.TrainerResponse;
import com.skating.platform.backend.dto.trainerService.response.TrainerServiceResponse;
import com.skating.platform.backend.entity.*;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.TrainerMapper;
import com.skating.platform.backend.repository.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.skating.platform.backend.mapper.TrainerServiceMapper;
import com.skating.platform.backend.repository.TrainerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainerService {
    private final TrainerServiceMapper trainerServiceMapper;
    private final TrainerRepository repository;
    private final TrainerMapper mapper;
    private final SchoolServiceService schoolServiceService;
    private final AppUserRepository appUserRepository;


    public TrainerService(TrainerRepository repository, TrainerMapper mapper, SchoolServiceService schoolServiceService,
                          TrainerServiceMapper trainerServiceMapper, AppUserRepository appUserRepository){
        this.repository = repository;
        this.mapper = mapper;
        this.schoolServiceService = schoolServiceService;
        this.trainerServiceMapper = trainerServiceMapper;
        this.appUserRepository = appUserRepository;
    }


    private Trainer getCurrentTrainerEntity(String userEmail) {

        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        Trainer trainer = user.getTrainer();

        if (trainer == null) {
            throw new ResourceNotFoundException(
                    "Trainer profile not found"
            );
        }

        return trainer;
    }

    @Transactional(readOnly = true)
    public TrainerResponse getCurrentTrainer(String userEmail) {

        Trainer trainer = getCurrentTrainerEntity(userEmail);

        return mapper.toResponse(trainer);
    }

    public Page<TrainerResponse> getAllTrainers(Pageable pageable){
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }
    @Transactional
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
    @Transactional
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
    @Transactional(readOnly = true)
    public TrainerServiceResponse getCurrentTrainerServices(
            String userEmail
    ) {

        Trainer trainer = getCurrentTrainerEntity(userEmail);

        return trainerServiceMapper.toResponse(trainer);
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

    public Page<TrainerResponse> searchTrainers(String query, Pageable pageable){
        return repository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query, pageable)
                .map(mapper::toResponse);
    }


}
