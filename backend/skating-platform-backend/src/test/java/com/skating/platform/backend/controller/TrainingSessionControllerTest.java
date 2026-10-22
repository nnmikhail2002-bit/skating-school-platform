package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.trainingsession.request.CreateTrainingSessionRequest;
import com.skating.platform.backend.dto.trainingsession.request.UpdateTrainingSessionRequest;
import com.skating.platform.backend.dto.trainingsession.response.TrainingSessionResponse;
import com.skating.platform.backend.entity.TrainingSessionStatus;
import com.skating.platform.backend.exception.BadRequestException;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.security.SecurityConfig;
import com.skating.platform.backend.service.TrainingSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(TrainingSessionController.class)
@Import(SecurityConfig.class)
class TrainingSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrainingSessionService trainingSessionService;


    // ---------- HELPERS ----------

    private RequestPostProcessor userJwt(String email, String role) {
        return jwt()
                .jwt(jwt -> jwt
                        .tokenValue("test-token")
                        .subject(email)
                        .claim("userId", 1L)
                        .claim("role", role)
                )
                .authorities(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );
    }

    private TrainingSessionResponse response() {
        return new TrainingSessionResponse(
                1L,
                2L,
                3L,
                LocalDateTime.of(2026, 10, 20, 15, 0),
                LocalDateTime.of(2026, 10, 20, 16, 0),
                10,
                TrainingSessionStatus.SCHEDULED
        );
    }

    private Page<TrainingSessionResponse> page() {
        return new PageImpl<>(
                List.of(response()),
                PageRequest.of(0, 10),
                1
        );
    }

    private String createJson() {
        return """
                {
                    "trainerId": 2,
                    "serviceId": 3,
                    "startTime": "2026-10-20T15:00:00",
                    "endTime": "2026-10-20T16:00:00",
                    "capacity": 10
                }
                """;
    }

    private String updateJson() {
        return """
                {
                    "trainerId": 2,
                    "serviceId": 3,
                    "startTime": "2026-10-20T15:00:00",
                    "endTime": "2026-10-20T16:00:00",
                    "capacity": 10,
                    "status": "SCHEDULED"
                }
                """;
    }


    @Test
    void shouldGetAllTrainingSessionsSuccessfully() throws Exception {

        when(trainingSessionService.getAllTrainingSessions(any(Pageable.class)))
                .thenReturn(page());

        mockMvc.perform(
                        get("/api/training-sessions")
                                .with(userJwt(
                                        "student@test.com",
                                        "STUDENT"
                                ))
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].trainerId").value(2));
    }

    @Test
    void shouldReturn401WhenAuthenticationIsMissing() throws Exception {

        mockMvc.perform(get("/api/training-sessions"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldGetCurrentTrainerSessionsSuccessfully() throws Exception {

        when(trainingSessionService.getCurrentTrainingSessions(eq("trainer@test.com"), any(Pageable.class))).
                thenReturn(page());

        mockMvc.perform(
                        get("/api/training-sessions/me")
                                .with(userJwt(
                                        "trainer@test.com",
                                        "TRAINER"
                                ))
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void shouldReturn404WhenTrainerProfileNotFound() throws Exception {

        when(trainingSessionService.getCurrentTrainingSessions(eq("trainer@test.com"), any(Pageable.class))).
                thenThrow(new ResourceNotFoundException("Trainer profile not found"));

        mockMvc.perform(
                        get("/api/training-sessions/me")
                                .with(userJwt(
                                        "trainer@test.com",
                                        "TRAINER"
                                ))
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn403WhenStudentRequestsTrainerSessions() throws Exception {

        mockMvc.perform(
                        get("/api/training-sessions/me")
                                .with(userJwt(
                                        "student@test.com",
                                        "STUDENT"
                                ))
                )
                .andExpect(status().isForbidden());
    }


    @Test
    void shouldGetTrainingSessionByIdSuccessfully() throws Exception {

        when(trainingSessionService.getTrainingSessionById(1L))
                .thenReturn(response());

        mockMvc.perform(
                        get("/api/training-sessions/1")
                                .with(userJwt(
                                        "student@test.com",
                                        "STUDENT"
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status")
                        .value("SCHEDULED"));
    }

    @Test
    void shouldReturn404WhenTrainingSessionNotFound() throws Exception {

        when(trainingSessionService.getTrainingSessionById(1L))
                .thenThrow(new ResourceNotFoundException("Training Session not found"));

        mockMvc.perform(
                        get("/api/training-sessions/1")
                                .with(userJwt(
                                        "student@test.com",
                                        "STUDENT"
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }


    @Test
    void shouldCreateTrainingSessionSuccessfully() throws Exception {

        when(trainingSessionService.createTrainingSession(any(CreateTrainingSessionRequest.class)))
                .thenReturn(response());

        mockMvc.perform(
                        post("/api/training-sessions")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(createJson())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status")
                        .value("SCHEDULED"));
    }

    @Test
    void shouldRejectCreateWithInvalidData() throws Exception {

        String json = """
                {
                    "trainerId": 2,
                    "serviceId": 3,
                    "startTime": "2026-10-20T15:00:00",
                    "endTime": "2026-10-20T16:00:00",
                    "capacity": 0
                }
                """;

        mockMvc.perform(
                        post("/api/training-sessions")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn403WhenStudentCreatesTrainingSession() throws Exception {

        mockMvc.perform(
                        post("/api/training-sessions")
                                .with(userJwt(
                                        "student@test.com",
                                        "STUDENT"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(createJson())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenTrainerOrServiceNotFoundOnCreate() throws Exception {

        when(trainingSessionService.createTrainingSession(any(CreateTrainingSessionRequest.class))).
                thenThrow(new ResourceNotFoundException("Trainer not found"));

        mockMvc.perform(
                        post("/api/training-sessions")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(createJson())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn409WhenTrainingSessionConflicts() throws Exception {

        when(trainingSessionService.createTrainingSession(any(CreateTrainingSessionRequest.class))).
                thenThrow(new ConflictException("Trainer already has a training session at this time"));

        mockMvc.perform(
                        post("/api/training-sessions")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(createJson())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }


    @Test
    void shouldUpdateTrainingSessionSuccessfully() throws Exception {

        when(trainingSessionService.updateTrainingSession(eq(1L), any(UpdateTrainingSessionRequest.class)))
                .thenReturn(response());

        mockMvc.perform(
                        put("/api/training-sessions/1")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(updateJson())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldRejectUpdateWithInvalidData() throws Exception {

        String json = """
                {
                    "trainerId": 2,
                    "serviceId": 3,
                    "startTime": "2026-10-20T15:00:00",
                    "endTime": "2026-10-20T16:00:00",
                    "capacity": 0,
                    "status": "SCHEDULED"
                }
                """;

        mockMvc.perform(
                        put("/api/training-sessions/1")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn403WhenTrainerUpdatesTrainingSession() throws Exception {

        mockMvc.perform(
                        put("/api/training-sessions/1")
                                .with(userJwt(
                                        "trainer@test.com",
                                        "TRAINER"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(updateJson())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn409WhenUpdateConflicts() throws Exception {

        when(trainingSessionService.updateTrainingSession(eq(1L), any(UpdateTrainingSessionRequest.class)))
                .thenThrow(new ConflictException("Trainer already has a training session at this time"));

        mockMvc.perform(
                        put("/api/training-sessions/1")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(updateJson())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }


    @Test
    void shouldDeleteTrainingSessionSuccessfully() throws Exception {

        mockMvc.perform(
                        delete("/api/training-sessions/1")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn403WhenTrainerDeletesTrainingSession() throws Exception {

        mockMvc.perform(
                        delete("/api/training-sessions/1")
                                .with(userJwt(
                                        "trainer@test.com",
                                        "TRAINER"
                                ))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenDeletingUnknownTrainingSession() throws Exception {

        doThrow(new ResourceNotFoundException("Training Session not found"))
                .when(trainingSessionService)
                .deleteTrainingSession(1L);

        mockMvc.perform(
                        delete("/api/training-sessions/1")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}