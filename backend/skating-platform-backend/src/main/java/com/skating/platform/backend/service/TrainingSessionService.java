package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.trainingsession.request.CreateTrainingSessionRequest;
import com.skating.platform.backend.dto.trainingsession.request.UpdateTrainingSessionRequest;
import com.skating.platform.backend.dto.trainingsession.response.TrainingSessionResponse;
import com.skating.platform.backend.entity.SchoolService;
import com.skating.platform.backend.entity.Trainer;
import com.skating.platform.backend.entity.TrainingSession;
import com.skating.platform.backend.entity.TrainingSessionStatus;
import com.skating.platform.backend.exception.BadRequestException;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.TrainingSessionMapper;
import com.skating.platform.backend.repository.SchoolServiceRepository;
import com.skating.platform.backend.repository.TrainerRepository;
import com.skating.platform.backend.repository.TrainingSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TrainingSessionService {
    private final TrainingSessionRepository repository;
    private final TrainerRepository trainerRepository;
    private final SchoolServiceRepository serviceRepository;
    private final TrainingSessionMapper mapper;

    public TrainingSessionService(TrainingSessionMapper mapper, TrainingSessionRepository repository, TrainerRepository trainerRepository, SchoolServiceRepository serviceRepository) {
        this.mapper = mapper;
        this.repository = repository;
        this.trainerRepository = trainerRepository;
        this.serviceRepository = serviceRepository;
    }

    private void validateTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException(
                    "Start time must be before end time"
            );
        }
    }

    private void validateTrainerService(Trainer trainer, SchoolService service) {
        boolean canProvideService = trainer.getServices()
                .stream()
                .anyMatch(trainerService -> trainerService.getId().equals(service.getId())
                );

        if (!canProvideService) {
            throw new BadRequestException(
                    "Trainer does not provide this service"
            );
        }
    }

    private void validateSchedule(
            Long trainerId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long excludeId
    ){

        long overlaps = repository.countOverlappingSessions(
                trainerId,
                startTime,
                endTime,
                excludeId,
                TrainingSessionStatus.CANCELLED
        );

        if(overlaps > 0){
            throw new ConflictException(
                    "Trainer already has a training session at this time"
            );
        }
    }

    @Transactional
    public TrainingSessionResponse createTrainingSession(CreateTrainingSessionRequest request) {
        TrainingSession session = new TrainingSession();
        Trainer trainer = trainerRepository.findById(request.getTrainerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Trainer not found")
                );
        SchoolService service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Service not found")
                );
        session.setTrainer(trainer);
        session.setService(service);
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setCapacity(request.getCapacity());
        session.setStatus(TrainingSessionStatus.SCHEDULED);
        validateTime(request.getStartTime(), request.getEndTime());
        validateTrainerService(trainer, service);
        validateSchedule(
               request.getTrainerId(),
                request.getStartTime(),
                request.getEndTime(),
                null
        );
        TrainingSession saved = repository.save(session);
        return mapper.toResponse(saved);
    }

    TrainingSession getEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Training Session not found")
                );
    }
    @Transactional(readOnly = true)
    public TrainingSessionResponse getTrainingSessionById(Long id) {
        TrainingSession trainingSession = getEntityById(id);
        return mapper.toResponse(trainingSession);
    }
    @Transactional(readOnly = true)
    public Page<TrainingSessionResponse> getAllTrainingSessions(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Transactional
    public TrainingSessionResponse updateTrainingSession(Long id, UpdateTrainingSessionRequest request) {
        TrainingSession existing = getEntityById(id);

        Trainer trainer = trainerRepository.findById(request.getTrainerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Trainer not found")
                );
        SchoolService service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Service not found")
                );
        existing.setTrainer(trainer);
        existing.setService(service);
        existing.setStartTime(request.getStartTime());
        existing.setEndTime(request.getEndTime());
        existing.setCapacity(request.getCapacity());
        existing.setStatus(request.getStatus());
        validateTime(request.getStartTime(), request.getEndTime());
        validateTrainerService(trainer, service);
        validateSchedule(
                request.getTrainerId(),
                request.getStartTime(),
                request.getEndTime(),
                existing.getId()
        );
        TrainingSession saved = repository.save(existing);
        return mapper.toResponse(saved);
    }

    @Transactional
    public void deleteTrainingSession(Long id) {
        TrainingSession trainingSession = getEntityById(id);
        repository.delete(trainingSession);
    }
}
