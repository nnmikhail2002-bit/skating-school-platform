package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.service.request.CreateSchoolServiceRequest;
import com.skating.platform.backend.dto.service.request.UpdateSchoolServiceRequest;
import com.skating.platform.backend.dto.service.response.SchoolServiceResponse;
import com.skating.platform.backend.entity.SchoolService;
import com.skating.platform.backend.entity.Trainer;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.SchoolServiceMapper;
import com.skating.platform.backend.repository.SchoolServiceRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class SchoolServiceServiceTest {

    @Mock
    private SchoolServiceRepository repository;

    @Mock
    private SchoolServiceMapper mapper;

    @InjectMocks
    private SchoolServiceService schoolServiceService;


    private SchoolService schoolService() {
        return new SchoolService(
                "Individual training",
                "Test description",
                new BigDecimal("5000.00"),
                "INDIVIDUAL"
        );
    }


    // GET ALL //

    @Test
    void shouldGetAllServicesSuccessfully() {

        Pageable pageable = PageRequest.of(0, 10);

        SchoolService service = schoolService();
        SchoolServiceResponse response = mock(SchoolServiceResponse.class);

        Page<SchoolService> services =
                new PageImpl<>(List.of(service), pageable, 1);

        when(repository.findAll(pageable))
                .thenReturn(services);

        when(mapper.toResponse(service))
                .thenReturn(response);


        Page<SchoolServiceResponse> result = schoolServiceService.getAllServices(pageable);


        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().get(0));

        verify(repository).findAll(pageable);
        verify(mapper).toResponse(service);
    }


    // GET BY ID //

    @Test
    void shouldGetServiceByIdSuccessfully() {

        SchoolService service = schoolService();

        SchoolServiceResponse response = mock(SchoolServiceResponse.class);

        when(repository.findById(3L))
                .thenReturn(Optional.of(service));

        when(mapper.toResponse(service))
                .thenReturn(response);


        SchoolServiceResponse result = schoolServiceService.getServiceById(3L);


        assertSame(response, result);

        verify(repository).findById(3L);
        verify(mapper).toResponse(service);
    }


    @Test
    void shouldRejectGetServiceByIdWhenNotFound() {

        when(repository.findById(3L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> schoolServiceService.getServiceById(3L));


        assertEquals(
                "Service not found",
                exception.getMessage()
        );

        verifyNoInteractions(mapper);
    }


    // CREATE //

    @Test
    void shouldCreateServiceSuccessfully() {

        CreateSchoolServiceRequest request = mock(CreateSchoolServiceRequest.class);

        SchoolService service = schoolService();

        SchoolServiceResponse response = mock(SchoolServiceResponse.class);


        when(mapper.toEntity(request))
                .thenReturn(service);

        when(repository.save(service))
                .thenReturn(service);

        when(mapper.toResponse(service))
                .thenReturn(response);


        SchoolServiceResponse result = schoolServiceService.createService(request);


        assertSame(response, result);

        verify(mapper).toEntity(request);
        verify(repository).save(service);
        verify(mapper).toResponse(service);
    }


    // UPDATE //

    @Test
    void shouldUpdateServiceSuccessfully() {

        SchoolService existing = schoolService();

        OffsetDateTime oldUpdatedAt = existing.getUpdatedAt();

        UpdateSchoolServiceRequest request = mock(UpdateSchoolServiceRequest.class);

        SchoolServiceResponse response = mock(SchoolServiceResponse.class);

        when(repository.findById(3L))
                .thenReturn(Optional.of(existing));

        when(request.getName())
                .thenReturn("Group training");

        when(request.getType())
                .thenReturn("GROUP");

        when(request.getPrice())
                .thenReturn(new BigDecimal("3000.00"));

        when(request.getDescription())
                .thenReturn("Updated description");

        when(repository.save(existing))
                .thenReturn(existing);

        when(mapper.toResponse(existing))
                .thenReturn(response);


        SchoolServiceResponse result = schoolServiceService.updateService(3L, request);


        assertSame(response, result);

        assertEquals(
                "Group training",
                existing.getName()
        );

        assertEquals(
                "GROUP",
                existing.getType()
        );

        assertEquals(
                new BigDecimal("3000.00"),
                existing.getPrice()
        );

        assertEquals(
                "Updated description",
                existing.getDescription()
        );

        assertNotNull(existing.getUpdatedAt());

        assertFalse(existing.getUpdatedAt().isBefore(oldUpdatedAt));

        verify(repository).save(existing);
    }


    @Test
    void shouldRejectUpdateWhenServiceNotFound() {

        UpdateSchoolServiceRequest request = mock(UpdateSchoolServiceRequest.class);

        when(repository.findById(3L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class,
                        () -> schoolServiceService.updateService(3L, request)
                );


        assertEquals(
                "Service not found",
                exception.getMessage()
        );

        verify(repository, never()).save(any(SchoolService.class));
    }


    // DELETE //

    @Test
    void shouldDeleteServiceSuccessfully() {

        SchoolService service = schoolService();

        Trainer trainer = new Trainer(
                2L,
                "Trainer",
                "Test",
                "89999999999",
                10,
                true,
                new HashSet<>()
        );

        trainer.getServices().add(service);

        service.getTrainers().add(trainer);


        when(repository.findById(3L)).thenReturn(Optional.of(service));


        schoolServiceService.deleteService(3L);


        assertFalse(
                trainer.getServices()
                        .contains(service)
        );

        verify(repository).delete(service);
    }


    @Test
    void shouldRejectDeleteWhenServiceNotFound() {

        when(repository.findById(3L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> schoolServiceService.deleteService(3L));

        assertEquals(
                "Service not found",
                exception.getMessage()
        );

        verify(repository, never()).delete(any(SchoolService.class));
    }


    // SEARCH //

    @Test
    void shouldSearchServicesSuccessfully() {

        String query = "train";

        Pageable pageable = PageRequest.of(0, 10);

        SchoolService service = schoolService();

        SchoolServiceResponse response = mock(SchoolServiceResponse.class);

        Page<SchoolService> services = new PageImpl<>(List.of(service), pageable, 1);


        when(repository
                .findByNameContainingIgnoreCaseOrTypeContainingIgnoreCase(query, query, pageable))
                .thenReturn(services);

        when(mapper.toResponse(service))
                .thenReturn(response);


        Page<SchoolServiceResponse> result = schoolServiceService.searchServices(query, pageable);


        assertEquals(
                1,
                result.getTotalElements()
        );

        assertSame(
                response,
                result.getContent().get(0)
        );

        verify(repository).findByNameContainingIgnoreCaseOrTypeContainingIgnoreCase(query, query, pageable);
    }


    // FILTER //

    @Test
    void shouldFilterServicesSuccessfully() {

        Pageable pageable = PageRequest.of(0, 10);

        SchoolService service = schoolService();

        SchoolServiceResponse response = mock(SchoolServiceResponse.class);

        Page<SchoolService> services = new PageImpl<>(List.of(service), pageable, 1);


        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(services);

        when(mapper.toResponse(service))
                .thenReturn(response);


        Page<SchoolServiceResponse> result =
                schoolServiceService.filterServices(
                        "train",
                        "INDIVIDUAL",
                        new BigDecimal("1000"),
                        new BigDecimal("6000"),
                        true,
                        pageable
                );


        assertEquals(
                1,
                result.getTotalElements()
        );

        assertSame(
                response,
                result.getContent().get(0)
        );

        verify(repository).findAll(any(Specification.class), eq(pageable));
    }


    @Test
    void shouldFilterServicesWithEmptyFiltersSuccessfully() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<SchoolService> services = new PageImpl<>(List.of(), pageable, 0);


        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(services);


        Page<SchoolServiceResponse> result =
                schoolServiceService.filterServices(
                        null,
                        null,
                        null,
                        null,
                        null,
                        pageable
                );


        assertEquals(
                0,
                result.getTotalElements()
        );

        verify(repository).findAll(any(Specification.class), eq(pageable));
    }
}