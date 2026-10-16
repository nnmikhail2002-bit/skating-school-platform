package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.trainer.request.CreateTrainerRequest;
import com.skating.platform.backend.dto.trainer.request.UpdateTrainerRequest;
import com.skating.platform.backend.dto.trainer.response.TrainerResponse;
import com.skating.platform.backend.dto.trainerService.response.TrainerServiceResponse;
import com.skating.platform.backend.entity.AppUser;
import com.skating.platform.backend.entity.SchoolService;
import com.skating.platform.backend.entity.Trainer;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.TrainerMapper;
import com.skating.platform.backend.mapper.TrainerServiceMapper;
import com.skating.platform.backend.repository.AppUserRepository;
import com.skating.platform.backend.repository.TrainerRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TrainerServiceTest {

    @Mock
    private TrainerRepository repository;

    @Mock
    private TrainerMapper mapper;

    @Mock
    private TrainerServiceMapper trainerServiceMapper;

    @Mock
    private SchoolServiceService schoolServiceService;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private TrainerService trainerService;


    private Trainer trainer() {
        Trainer trainer = new Trainer();

        trainer.setId(2L);
        trainer.setFirstName("Trainer");
        trainer.setLastName("Test");
        trainer.setPhone("89999999999");
        trainer.setExperienceYears(10);
        trainer.setActive(true);
        trainer.setServices(new HashSet<>());

        return trainer;
    }


    private AppUser user(String email, Trainer trainer) {
        AppUser user = new AppUser();

        user.setEmail(email);
        user.setTrainer(trainer);

        return user;
    }


    // CURRENT TRAINER //

    @Test
    void shouldGetCurrentTrainerSuccessfully() {

        String email = "trainer@test.com";

        Trainer trainer = trainer();
        AppUser user = user(email, trainer);

        TrainerResponse response = mock(TrainerResponse.class);

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(mapper.toResponse(trainer))
                .thenReturn(response);


        TrainerResponse result = trainerService.getCurrentTrainer(email);


        assertSame(response, result);

        verify(appUserRepository).findByEmail(email);
        verify(mapper).toResponse(trainer);
    }


    @Test
    void shouldRejectCurrentTrainerWhenUserNotFound() {

        String email = "missing@test.com";

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> trainerService.getCurrentTrainer(email));


        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verifyNoInteractions(mapper);
    }


    @Test
    void shouldRejectCurrentTrainerWhenProfileNotFound() {

        String email = "student@test.com";

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setTrainer(null);

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> trainerService.getCurrentTrainer(email));

        assertEquals(
                "Trainer profile not found",
                exception.getMessage()
        );

        verifyNoInteractions(mapper);
    }


    // CREATE //

    @Test
    void shouldCreateTrainerSuccessfully() {

        CreateTrainerRequest request = mock(CreateTrainerRequest.class);

        Trainer trainer = trainer();

        TrainerResponse response = mock(TrainerResponse.class);


        when(request.getPhone())
                .thenReturn("89999999999");

        when(repository.existsByPhone("89999999999"))
                .thenReturn(false);

        when(mapper.toEntity(request))
                .thenReturn(trainer);

        when(repository.save(trainer))
                .thenReturn(trainer);

        when(mapper.toResponse(trainer))
                .thenReturn(response);


        TrainerResponse result = trainerService.createTrainer(request);

        assertSame(response, result);

        verify(repository).existsByPhone("89999999999");
        verify(mapper).toEntity(request);
        verify(repository).save(trainer);
    }


    @Test
    void shouldRejectCreateWhenPhoneAlreadyExists() {

        CreateTrainerRequest request = mock(CreateTrainerRequest.class);

        when(request.getPhone())
                .thenReturn("89999999999");

        when(repository.existsByPhone("89999999999"))
                .thenReturn(true);


        ConflictException exception =
                assertThrows(ConflictException.class, () -> trainerService.createTrainer(request));


        assertEquals(
                "Trainer with this phone already exists",
                exception.getMessage()
        );

        verify(repository, never()).save(any(Trainer.class));

        verifyNoInteractions(mapper);
    }


    // GET BY ID //

    @Test
    void shouldGetTrainerByIdSuccessfully() {

        Trainer trainer = trainer();

        TrainerResponse response = mock(TrainerResponse.class);

        when(repository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(mapper.toResponse(trainer))
                .thenReturn(response);

        TrainerResponse result = trainerService.getTrainerById(2L);

        assertSame(response, result);

        verify(repository).findById(2L);
        verify(mapper).toResponse(trainer);
    }


    @Test
    void shouldRejectGetTrainerByIdWhenNotFound() {

        when(repository.findById(2L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> trainerService.getTrainerById(2L));


        assertEquals(
                "Trainer not found",
                exception.getMessage()
        );

        verifyNoInteractions(mapper);
    }


    // GET ALL //

    @Test
    void shouldGetAllTrainersSuccessfully() {

        Pageable pageable = PageRequest.of(0, 10);

        Trainer trainer = trainer();

        TrainerResponse response = mock(TrainerResponse.class);

        Page<Trainer> trainers =
                new PageImpl<>(List.of(trainer), pageable, 1);


        when(repository.findAll(pageable))
                .thenReturn(trainers);

        when(mapper.toResponse(trainer))
                .thenReturn(response);


        Page<TrainerResponse> result = trainerService.getAllTrainers(pageable);


        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().get(0));
    }


    // UPDATE //

    @Test
    void shouldUpdateTrainerSuccessfully() {

        Trainer existing = trainer();

        UpdateTrainerRequest request = mock(UpdateTrainerRequest.class);

        TrainerResponse response = mock(TrainerResponse.class);


        when(repository.findById(2L))
                .thenReturn(Optional.of(existing));

        when(request.getPhone())
                .thenReturn("88888888888");

        when(repository.existsByPhone("88888888888"))
                .thenReturn(false);

        when(repository.save(existing))
                .thenReturn(existing);

        when(mapper.toResponse(existing))
                .thenReturn(response);


        TrainerResponse result = trainerService.updateTrainer(2L, request);


        assertSame(response, result);

        verify(mapper).updateEntity(existing, request);

        verify(repository).save(existing);
    }


    @Test
    void shouldAllowUpdateWithSamePhone() {

        Trainer existing = trainer();

        UpdateTrainerRequest request = mock(UpdateTrainerRequest.class);


        when(repository.findById(2L))
                .thenReturn(Optional.of(existing));

        when(request.getPhone())
                .thenReturn("89999999999");

        when(repository.existsByPhone("89999999999"))
                .thenReturn(true);

        when(repository.save(existing))
                .thenReturn(existing);


        trainerService.updateTrainer(2L, request);


        verify(mapper).updateEntity(existing, request);

        verify(repository).save(existing);
    }


    @Test
    void shouldRejectUpdateWhenPhoneAlreadyExists() {

        Trainer existing = trainer();

        UpdateTrainerRequest request = mock(UpdateTrainerRequest.class);


        when(repository.findById(2L))
                .thenReturn(Optional.of(existing));

        when(request.getPhone())
                .thenReturn("88888888888");

        when(repository.existsByPhone("88888888888"))
                .thenReturn(true);


        ConflictException exception =
                assertThrows(ConflictException.class, () -> trainerService.updateTrainer(2L, request));


        assertEquals(
                "Trainer with this phone already exists",
                exception.getMessage()
        );

        verify(mapper, never()).updateEntity(any(), any());

        verify(repository, never()).save(any(Trainer.class));
    }


    @Test
    void shouldRejectUpdateWhenTrainerNotFound() {

        UpdateTrainerRequest request = mock(UpdateTrainerRequest.class);

        when(repository.findById(2L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> trainerService.updateTrainer(2L, request));


        assertEquals(
                "Trainer not found",
                exception.getMessage()
        );

        verify(repository, never()).save(any(Trainer.class));
    }


    // DELETE TRAINER //

    @Test
    void shouldDeleteTrainerSuccessfully() {

        Trainer trainer = trainer();

        SchoolService schoolService = mock(SchoolService.class);

        trainer.getServices()
                .add(schoolService);

        when(repository.findById(2L))
                .thenReturn(Optional.of(trainer));


        trainerService.deleteTrainer(2L);


        assertTrue(trainer.getServices().isEmpty());

        verify(repository).delete(trainer);
    }


    @Test
    void shouldRejectDeleteWhenTrainerNotFound() {

        when(repository.findById(2L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> trainerService.deleteTrainer(2L));


        assertEquals(
                "Trainer not found",
                exception.getMessage()
        );

        verify(repository, never()).delete(any(Trainer.class));
    }


    // ADD SERVICE //

    @Test
    void shouldAddServiceToTrainerSuccessfully() {

        Trainer trainer = trainer();

        SchoolService schoolService = mock(SchoolService.class);


        when(repository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(schoolServiceService.getEntityById(3L))
                .thenReturn(schoolService);


        trainerService.addServiceToTrainer(2L, 3L);


        assertTrue(trainer.getServices().contains(schoolService));

        verify(repository).save(trainer);
    }


    @Test
    void shouldRejectAddServiceWhenTrainerNotFound() {

        when(repository.findById(2L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class,
                        () -> trainerService.addServiceToTrainer(2L, 3L));


        assertEquals(
                "Trainer not found",
                exception.getMessage()
        );

        verifyNoInteractions(schoolServiceService);
    }


    // TRAINER SERVICES //

    @Test
    void shouldGetAllTrainersServicesSuccessfully() {

        Pageable pageable = PageRequest.of(0, 10);

        Trainer trainer = trainer();

        TrainerServiceResponse response = mock(TrainerServiceResponse.class);

        Page<Trainer> trainers =
                new PageImpl<>(List.of(trainer), pageable, 1);


        when(repository.findAll(pageable))
                .thenReturn(trainers);

        when(trainerServiceMapper.toResponse(trainer))
                .thenReturn(response);


        Page<TrainerServiceResponse> result =
                trainerService.getAllTrainersServices(pageable);


        assertEquals(
                1,
                result.getTotalElements()
        );

        assertSame(
                response,
                result.getContent().get(0)
        );
    }


    @Test
    void shouldGetCurrentTrainerServicesSuccessfully() {

        String email = "trainer@test.com";

        Trainer trainer = trainer();

        AppUser user = user(email, trainer);

        TrainerServiceResponse response = mock(TrainerServiceResponse.class);


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(trainerServiceMapper.toResponse(trainer))
                .thenReturn(response);


        TrainerServiceResponse result =
                trainerService.getCurrentTrainerServices(email);


        assertSame(response, result);
    }


    @Test
    void shouldRejectCurrentTrainerServicesWhenUserNotFound() {

        String email = "missing@test.com";

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class,
                        () -> trainerService.getCurrentTrainerServices(email));


        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verifyNoInteractions(trainerServiceMapper);
    }


    @Test
    void shouldRejectCurrentTrainerServicesWhenProfileNotFound() {

        String email = "student@test.com";

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setTrainer(null);


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class,
                        () -> trainerService.getCurrentTrainerServices(email));


        assertEquals(
                "Trainer profile not found",
                exception.getMessage()
        );

        verifyNoInteractions(trainerServiceMapper);
    }


    @Test
    void shouldGetAllTrainersWithServicesSuccessfully() {

        Pageable pageable = PageRequest.of(0, 10);

        Trainer trainer = trainer();

        TrainerServiceResponse response = mock(TrainerServiceResponse.class);

        Page<Trainer> trainers =
                new PageImpl<>(List.of(trainer), pageable, 1);


        when(repository.findDistinctByServicesIsNotEmpty(pageable))
                .thenReturn(trainers);

        when(trainerServiceMapper
                .toResponse(trainer))
                .thenReturn(response);


        Page<TrainerServiceResponse> result = trainerService.getAllTrainersWithServices(pageable);


        assertEquals(
                1,
                result.getTotalElements()
        );

        assertSame(
                response,
                result.getContent().get(0)
        );
    }


    @Test
    void shouldGetTrainerServiceByIdSuccessfully() {

        Trainer trainer = trainer();

        TrainerServiceResponse response = mock(TrainerServiceResponse.class);


        when(repository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(trainerServiceMapper
                .toResponse(trainer))
                .thenReturn(response);


        TrainerServiceResponse result = trainerService.getTrainerServiceById(2L);


        assertSame(response, result);
    }


    @Test
    void shouldRejectTrainerServiceByIdWhenTrainerNotFound() {

        when(repository.findById(2L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class,
                        () -> trainerService.getTrainerServiceById(2L));


        assertEquals(
                "Trainer not found",
                exception.getMessage()
        );

        verifyNoInteractions(trainerServiceMapper);
    }


    // DELETE SERVICE //

    @Test
    void shouldDeleteTrainerServiceSuccessfully() {

        Trainer trainer = trainer();

        SchoolService schoolService = mock(SchoolService.class);

        trainer.getServices()
                .add(schoolService);


        when(repository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(schoolServiceService.getEntityById(3L))
                .thenReturn(schoolService);


        trainerService.deleteTrainerService(2L, 3L);


        assertFalse(
                trainer.getServices()
                .contains(schoolService)
        );

        verify(repository).save(trainer);
    }


    @Test
    void shouldRejectDeleteTrainerServiceWhenTrainerNotFound() {

        when(repository.findById(2L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> trainerService.deleteTrainerService(2L, 3L)
                );


        assertEquals(
                "Trainer not found",
                exception.getMessage()
        );

        verifyNoInteractions(schoolServiceService);
    }


    // SEARCH //

    @Test
    void shouldSearchTrainersSuccessfully() {

        String query = "Tra";

        Pageable pageable = PageRequest.of(0, 10);

        Trainer trainer = trainer();

        TrainerResponse response = mock(TrainerResponse.class);

        Page<Trainer> trainers =
                new PageImpl<>(List.of(trainer), pageable, 1);


        when(repository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query, pageable))
                .thenReturn(trainers);

        when(mapper.toResponse(trainer))
                .thenReturn(response);


        Page<TrainerResponse> result =
                trainerService.searchTrainers(query, pageable);


        assertEquals(
                1,
                result.getTotalElements()
        );

        assertSame(
                response,
                result.getContent().get(0)
        );
    }
}