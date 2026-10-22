package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.trainingsession.request.CreateTrainingSessionRequest;
import com.skating.platform.backend.dto.trainingsession.request.UpdateTrainingSessionRequest;
import com.skating.platform.backend.dto.trainingsession.response.TrainingSessionResponse;
import com.skating.platform.backend.entity.*;
import com.skating.platform.backend.exception.BadRequestException;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.TrainingSessionMapper;
import com.skating.platform.backend.repository.AppUserRepository;
import com.skating.platform.backend.repository.SchoolServiceRepository;
import com.skating.platform.backend.repository.TrainerRepository;
import com.skating.platform.backend.repository.TrainingSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TrainingSessionServiceTest {

    @Mock
    private TrainingSessionRepository repository;

    @Mock
    private TrainerRepository trainerRepository;

    @Mock
    private SchoolServiceRepository serviceRepository;

    @Mock
    private TrainingSessionMapper mapper;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private TrainingSessionService trainingSessionService;

    @Test
    void shouldCreateTrainingSessionSuccessfully() {

        SchoolService schoolService = mock(SchoolService.class);

        when(schoolService.getId())
                .thenReturn(3L);


        Set<SchoolService> services = new HashSet<>();
        services.add(schoolService);

        Trainer trainer = new Trainer(
                2L,
                "firstname",
                "lastname",
                "89873943333",
                10,
                true,
                services
        );

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 20, 15, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 20, 16, 0);

        CreateTrainingSessionRequest request =
                new CreateTrainingSessionRequest();

        request.setTrainerId(2L);
        request.setServiceId(3L);
        request.setStartTime(start);
        request.setEndTime(end);
        request.setCapacity(10);

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(serviceRepository.findById(3L))
                .thenReturn(Optional.of(schoolService));

        when(repository.save(any(TrainingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TrainingSessionResponse response =
                new TrainingSessionResponse(
                        1L,
                        2L,
                        3L,
                        start,
                        end,
                        10,
                        TrainingSessionStatus.SCHEDULED
                );

        when(repository.countOverlappingSessions(
                2L,
                start,
                end,
                null,
                TrainingSessionStatus.CANCELLED
        )).thenReturn(0L);

        when(mapper.toResponse(any(TrainingSession.class)))
                .thenReturn(response);

        TrainingSessionResponse result =
                trainingSessionService.createTrainingSession(request);

        ArgumentCaptor<TrainingSession> captor =
                ArgumentCaptor.forClass(TrainingSession.class);

        verify(repository).save(captor.capture());

        TrainingSession savedSession = captor.getValue();

        assertEquals(1L, result.getId());
        assertEquals(2L, result.getTrainerId());
        assertEquals(3L, result.getServiceId());
        assertEquals(
                TrainingSessionStatus.SCHEDULED,
                result.getStatus()
        );

        assertEquals(trainer, savedSession.getTrainer());
        assertEquals(schoolService, savedSession.getService());
        assertEquals(start, savedSession.getStartTime());
        assertEquals(end, savedSession.getEndTime());
        assertEquals(10, savedSession.getCapacity());
        assertEquals(
                TrainingSessionStatus.SCHEDULED,
                savedSession.getStatus()
        );

    }

    @Test
    void shouldRejectWhenStartTimeIsNotBeforeEndTime() {

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 20, 16, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 20, 15, 0);


        Trainer trainer = new Trainer(
                2L,
                "firstname",
                "lastname",
                "89873943333",
                10,
                true,
                new HashSet<>()
        );

        SchoolService schoolService = mock(SchoolService.class);


        CreateTrainingSessionRequest request =
                new CreateTrainingSessionRequest();

        request.setTrainerId(2L);
        request.setServiceId(3L);
        request.setStartTime(start);
        request.setEndTime(end);
        request.setCapacity(10);


        when(trainerRepository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(serviceRepository.findById(3L))
                .thenReturn(Optional.of(schoolService));


        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> trainingSessionService
                                .createTrainingSession(request)
                );


        assertEquals(
                "Start time must be before end time",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(TrainingSession.class));
    }

    @Test
    void shouldRejectWhenTrainerDoesNotProvideService() {
        SchoolService schoolService = mock(SchoolService.class);

        when(schoolService.getId())
                .thenReturn(2L);

        SchoolService requestedService = mock(SchoolService.class);

        when(requestedService.getId())
                .thenReturn(3L);

        Set<SchoolService> services = new HashSet<>();
        services.add(schoolService);

        Trainer trainer = new Trainer(
                2L,
                "firstname",
                "lastname",
                "89873943333",
                10,
                true,
                services
        );

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 20, 15, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 20, 16, 0);

        CreateTrainingSessionRequest request =
                new CreateTrainingSessionRequest();

        request.setTrainerId(2L);
        request.setServiceId(3L);
        request.setStartTime(start);
        request.setEndTime(end);
        request.setCapacity(10);

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(serviceRepository.findById(3L))
                .thenReturn(Optional.of(requestedService));

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> trainingSessionService
                                .createTrainingSession(request)
                );


        assertEquals(
                "Trainer does not provide this service",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(TrainingSession.class));

    }


    @Test
    void shouldRejectWhenScheduleOverlaps() {
        SchoolService schoolService = mock(SchoolService.class);

        when(schoolService.getId())
                .thenReturn(2L);


        Set<SchoolService> services = new HashSet<>();
        services.add(schoolService);

        Trainer trainer = new Trainer(
                2L,
                "firstname",
                "lastname",
                "89873943333",
                10,
                true,
                services
        );

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 20, 15, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 20, 16, 0);

        CreateTrainingSessionRequest request =
                new CreateTrainingSessionRequest();

        request.setTrainerId(2L);
        request.setServiceId(2L);
        request.setStartTime(start);
        request.setEndTime(end);
        request.setCapacity(10);

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(serviceRepository.findById(2L))
                .thenReturn(Optional.of(schoolService));

        when(repository.countOverlappingSessions(
                2L,
                start,
                end,
                null,
                TrainingSessionStatus.CANCELLED
        )).thenReturn(1L);

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> trainingSessionService
                                .createTrainingSession(request)
                );


        assertEquals(
                "Trainer already has a training session at this time",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(TrainingSession.class));

    }

    @Test
    void shouldRejectWhenTrainerNotFound() {

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 20, 15, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 20, 16, 0);

        CreateTrainingSessionRequest request =
                new CreateTrainingSessionRequest();

        request.setTrainerId(2L);
        request.setServiceId(2L);
        request.setStartTime(start);
        request.setEndTime(end);
        request.setCapacity(10);

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> trainingSessionService
                                .createTrainingSession(request)
                );

        assertEquals(
                "Trainer not found",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(TrainingSession.class));

    }

    @Test
    void shouldRejectWhenServiceNotFound() {

        SchoolService schoolService = mock(SchoolService.class);

        Set<SchoolService> services = new HashSet<>();
        services.add(schoolService);

        Trainer trainer = new Trainer(
                2L,
                "firstname",
                "lastname",
                "89873943333",
                10,
                true,
                services
        );

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 20, 15, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 20, 16, 0);

        CreateTrainingSessionRequest request =
                new CreateTrainingSessionRequest();

        request.setTrainerId(2L);
        request.setStartTime(start);
        request.setServiceId(2L);
        request.setEndTime(end);
        request.setCapacity(10);

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(serviceRepository.findById(2L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> trainingSessionService
                                .createTrainingSession(request)
                );

        assertEquals(
                "Service not found",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(TrainingSession.class));

    }


    /// update ///


    @Test
    void shouldUpdateTrainingSessionSuccessfully() {

        SchoolService schoolService = mock(SchoolService.class);

        when(schoolService.getId())
                .thenReturn(3L);


        Set<SchoolService> services = new HashSet<>();
        services.add(schoolService);

        Trainer trainer = new Trainer(
                2L,
                "firstname",
                "lastname",
                "89873943333",
                10,
                true,
                services
        );

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 20, 15, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 20, 16, 0);

        TrainingSession existing = new TrainingSession();
        existing.setId(10L);

        UpdateTrainingSessionRequest request =
                new UpdateTrainingSessionRequest();

        request.setTrainerId(2L);
        request.setServiceId(3L);
        request.setStartTime(start);
        request.setEndTime(end);
        request.setCapacity(10);
        request.setStatus(TrainingSessionStatus.SCHEDULED);


          when(repository.findById(10L))
                .thenReturn(Optional.of(existing));

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(serviceRepository.findById(3L))
                .thenReturn(Optional.of(schoolService));

        when(repository.save(any(TrainingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TrainingSessionResponse response =
                new TrainingSessionResponse(
                        10L,
                        2L,
                        3L,
                        start,
                        end,
                        10,
                        TrainingSessionStatus.SCHEDULED
                );

        when(repository.countOverlappingSessions(
                2L,
                start,
                end,
                10L,
                TrainingSessionStatus.CANCELLED
        )).thenReturn(0L);

        when(mapper.toResponse(any(TrainingSession.class)))
                .thenReturn(response);

        TrainingSessionResponse result =
                trainingSessionService.updateTrainingSession(10L, request);

        ArgumentCaptor<TrainingSession> captor =
                ArgumentCaptor.forClass(TrainingSession.class);

        verify(repository).save(captor.capture());

        TrainingSession savedSession = captor.getValue();

        assertEquals(10L, result.getId());
        assertEquals(2L, result.getTrainerId());
        assertEquals(3L, result.getServiceId());
        assertEquals(
                TrainingSessionStatus.SCHEDULED,
                result.getStatus()
        );

        assertEquals(trainer, savedSession.getTrainer());
        assertEquals(schoolService, savedSession.getService());
        assertEquals(start, savedSession.getStartTime());
        assertEquals(end, savedSession.getEndTime());
        assertEquals(10, savedSession.getCapacity());
        assertEquals(
                TrainingSessionStatus.SCHEDULED,
                savedSession.getStatus()
        );

    }

    @Test
    void shouldRejectUpdateWhenTrainingSessionNotFound() {
        TrainingSession existing = new TrainingSession();
        existing.setId(10L);

        UpdateTrainingSessionRequest request =
                new UpdateTrainingSessionRequest();

        when(repository.findById(10L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> trainingSessionService
                                .updateTrainingSession(10L, request)
                );

        assertEquals(
                "Training Session not found",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(TrainingSession.class));

    }



    @Test
    void shouldRejectUpdateWhenStartTimeIsNotBeforeEndTime() {

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 20, 16, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 20, 15, 0);


        Trainer trainer = new Trainer(
                2L,
                "firstname",
                "lastname",
                "89873943333",
                10,
                true,
                new HashSet<>()
        );

        SchoolService schoolService = mock(SchoolService.class);

        TrainingSession existing = new TrainingSession();
        existing.setId(2L);

        UpdateTrainingSessionRequest request =
                new UpdateTrainingSessionRequest();



        request.setTrainerId(2L);
        request.setServiceId(3L);
        request.setStartTime(start);
        request.setEndTime(end);
        request.setCapacity(10);

        when(repository.findById(2L))
                .thenReturn(Optional.of(existing));

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(serviceRepository.findById(3L))
                .thenReturn(Optional.of(schoolService));


        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> trainingSessionService
                                .updateTrainingSession(2L, request)
                );


        assertEquals(
                "Start time must be before end time",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(TrainingSession.class));
    }

    @Test
    void shouldRejectUpdateWhenTrainerDoesNotProvideService() {
        SchoolService schoolService = mock(SchoolService.class);

        when(schoolService.getId())
                .thenReturn(2L);

        SchoolService requestedService = mock(SchoolService.class);

        when(requestedService.getId())
                .thenReturn(3L);

        Set<SchoolService> services = new HashSet<>();
        services.add(schoolService);

        Trainer trainer = new Trainer(
                2L,
                "firstname",
                "lastname",
                "89873943333",
                10,
                true,
                services
        );

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 20, 15, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 20, 16, 0);

        UpdateTrainingSessionRequest request =
                new UpdateTrainingSessionRequest();

        request.setTrainerId(2L);
        request.setServiceId(3L);
        request.setStartTime(start);
        request.setEndTime(end);
        request.setCapacity(10);

        TrainingSession existing = new TrainingSession();
        existing.setId(2L);

        when(repository.findById(2L))
                .thenReturn(Optional.of(existing));

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(serviceRepository.findById(3L))
                .thenReturn(Optional.of(requestedService));

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> trainingSessionService
                                .updateTrainingSession(2L, request)
                );


        assertEquals(
                "Trainer does not provide this service",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(TrainingSession.class));

    }


    @Test
    void shouldRejectUpdateWhenScheduleOverlaps() {
        SchoolService schoolService = mock(SchoolService.class);

        when(schoolService.getId())
                .thenReturn(2L);


        Set<SchoolService> services = new HashSet<>();
        services.add(schoolService);

        Trainer trainer = new Trainer(
                2L,
                "firstname",
                "lastname",
                "89873943333",
                10,
                true,
                services
        );

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 20, 15, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 20, 16, 0);

        UpdateTrainingSessionRequest request =
                new UpdateTrainingSessionRequest();

        request.setTrainerId(2L);
        request.setServiceId(2L);
        request.setStartTime(start);
        request.setEndTime(end);
        request.setCapacity(10);

        TrainingSession existing = new TrainingSession();
        existing.setId(2L);

        when(repository.findById(2L))
                .thenReturn(Optional.of(existing));

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(serviceRepository.findById(2L))
                .thenReturn(Optional.of(schoolService));

        when(repository.countOverlappingSessions(
                2L,
                start,
                end,
                2L,
                TrainingSessionStatus.CANCELLED
        )).thenReturn(1L);

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> trainingSessionService
                                .updateTrainingSession(2L, request)
                );


        assertEquals(
                "Trainer already has a training session at this time",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(TrainingSession.class));

    }

    @Test
    void shouldRejectUpdateWhenTrainerNotFound() {

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 20, 15, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 20, 16, 0);

        UpdateTrainingSessionRequest request =
                new UpdateTrainingSessionRequest();

        request.setTrainerId(2L);
        request.setServiceId(2L);
        request.setStartTime(start);
        request.setEndTime(end);
        request.setCapacity(10);

        TrainingSession existing = new TrainingSession();
        existing.setId(2L);

        when(repository.findById(2L))
                .thenReturn(Optional.of(existing));

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> trainingSessionService
                                .updateTrainingSession(2L, request)
                );

        assertEquals(
                "Trainer not found",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(TrainingSession.class));

    }

    @Test
    void shouldRejectUpdateWhenServiceNotFound() {

        SchoolService schoolService = mock(SchoolService.class);

        Set<SchoolService> services = new HashSet<>();
        services.add(schoolService);

        Trainer trainer = new Trainer(
                2L,
                "firstname",
                "lastname",
                "89873943333",
                10,
                true,
                services
        );

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 20, 15, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 20, 16, 0);

        UpdateTrainingSessionRequest request =
                new UpdateTrainingSessionRequest();

        request.setTrainerId(2L);
        request.setStartTime(start);
        request.setServiceId(2L);
        request.setEndTime(end);
        request.setCapacity(10);

        TrainingSession existing = new TrainingSession();
        existing.setId(2L);

        when(repository.findById(2L))
                .thenReturn(Optional.of(existing));

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(serviceRepository.findById(2L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> trainingSessionService
                                .updateTrainingSession(2L, request)
                );

        assertEquals(
                "Service not found",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(TrainingSession.class));

    }

    // GET BY ID //

    @Test
    void shouldGetTrainingSessionByIdSuccessfully() {

        TrainingSession session = new TrainingSession();
        session.setId(10L);

        TrainingSessionResponse response =
                new TrainingSessionResponse(
                        10L,
                        2L,
                        3L,
                        LocalDateTime.of(2026, 10, 20, 15, 0),
                        LocalDateTime.of(2026, 10, 20, 16, 0),
                        10,
                        TrainingSessionStatus.SCHEDULED
                );

        when(repository.findById(10L))
                .thenReturn(Optional.of(session));

        when(mapper.toResponse(session))
                .thenReturn(response);


        TrainingSessionResponse result =
                trainingSessionService.getTrainingSessionById(10L);


        assertEquals(10L, result.getId());
        assertEquals(2L, result.getTrainerId());
        assertEquals(3L, result.getServiceId());

        verify(repository).findById(10L);
        verify(mapper).toResponse(session);
    }


    @Test
    void shouldRejectGetByIdWhenTrainingSessionNotFound() {

        when(repository.findById(10L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> trainingSessionService
                                .getTrainingSessionById(10L)
                );


        assertEquals(
                "Training Session not found",
                exception.getMessage()
        );

        verifyNoInteractions(mapper);
    }


// GET ALL //

    @Test
    void shouldGetAllTrainingSessionsSuccessfully() {

        Pageable pageable = PageRequest.of(0, 10);

        TrainingSession session = new TrainingSession();
        session.setId(10L);

        Page<TrainingSession> sessions =
                new PageImpl<>(
                        List.of(session),
                        pageable,
                        1
                );

        TrainingSessionResponse response =
                new TrainingSessionResponse(
                        10L,
                        2L,
                        3L,
                        LocalDateTime.of(2026, 10, 20, 15, 0),
                        LocalDateTime.of(2026, 10, 20, 16, 0),
                        10,
                        TrainingSessionStatus.SCHEDULED
                );

        when(repository.findAll(pageable))
                .thenReturn(sessions);

        when(mapper.toResponse(session))
                .thenReturn(response);


        Page<TrainingSessionResponse> result =
                trainingSessionService.getAllTrainingSessions(pageable);


        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(10L, result.getContent().get(0).getId());

        verify(repository).findAll(pageable);
        verify(mapper).toResponse(session);
    }


// CURRENT TRAINER SESSIONS //

    @Test
    void shouldGetCurrentTrainingSessionsSuccessfully() {

        String email = "trainer@test.com";

        Pageable pageable = PageRequest.of(0, 10);

        Trainer trainer = new Trainer(
                2L,
                "firstname",
                "lastname",
                "89873943333",
                10,
                true,
                new HashSet<>()
        );

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setTrainer(trainer);

        TrainingSession session = new TrainingSession();
        session.setId(10L);

        Page<TrainingSession> sessions =
                new PageImpl<>(
                        List.of(session),
                        pageable,
                        1
                );

        TrainingSessionResponse response =
                new TrainingSessionResponse(
                        10L,
                        2L,
                        3L,
                        LocalDateTime.of(2026, 10, 20, 15, 0),
                        LocalDateTime.of(2026, 10, 20, 16, 0),
                        10,
                        TrainingSessionStatus.SCHEDULED
                );

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(repository.findByTrainer_Id(2L, pageable))
                .thenReturn(sessions);

        when(mapper.toResponse(session))
                .thenReturn(response);


        Page<TrainingSessionResponse> result =
                trainingSessionService
                        .getCurrentTrainingSessions(email, pageable);


        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).getId());

        verify(appUserRepository).findByEmail(email);
        verify(repository).findByTrainer_Id(2L, pageable);
    }


    @Test
    void shouldRejectGetCurrentTrainingSessionsWhenUserNotFound() {

        String email = "missing@test.com";

        Pageable pageable = PageRequest.of(0, 10);

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> trainingSessionService
                                .getCurrentTrainingSessions(email, pageable)
                );


        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verify(repository, never())
                .findByTrainer_Id(any(), any());
    }


    @Test
    void shouldRejectGetCurrentTrainingSessionsWhenTrainerProfileNotFound() {

        String email = "student@test.com";

        Pageable pageable = PageRequest.of(0, 10);

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setTrainer(null);

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> trainingSessionService
                                .getCurrentTrainingSessions(email, pageable)
                );


        assertEquals(
                "Trainer profile not found",
                exception.getMessage()
        );

        verify(repository, never())
                .findByTrainer_Id(any(), any());
    }


// DELETE //

    @Test
    void shouldDeleteTrainingSessionSuccessfully() {

        TrainingSession session = new TrainingSession();
        session.setId(10L);

        when(repository.findById(10L))
                .thenReturn(Optional.of(session));


        trainingSessionService.deleteTrainingSession(10L);


        verify(repository).findById(10L);
        verify(repository).delete(session);
    }


    @Test
    void shouldRejectDeleteWhenTrainingSessionNotFound() {

        when(repository.findById(10L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> trainingSessionService
                                .deleteTrainingSession(10L)
                );


        assertEquals(
                "Training Session not found",
                exception.getMessage()
        );

        verify(repository, never())
                .delete(any(TrainingSession.class));
    }

}