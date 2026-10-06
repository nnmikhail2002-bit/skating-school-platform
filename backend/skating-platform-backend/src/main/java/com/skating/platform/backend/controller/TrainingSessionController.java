package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.trainingsession.request.CreateTrainingSessionRequest;
import com.skating.platform.backend.dto.trainingsession.request.UpdateTrainingSessionRequest;
import com.skating.platform.backend.dto.trainingsession.response.TrainingSessionResponse;
import com.skating.platform.backend.service.TrainingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;


@Tag(name = "TrainingSession", description = "Management of training session")
@RestController
@RequestMapping("/api/training-sessions")
public class TrainingSessionController {
    private final TrainingSessionService service;

    public TrainingSessionController(TrainingSessionService service){this.service = service;}

    @Operation(
            summary = "Get all training sessions",
            description = "Returns training sessions with pagination and sorting"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training sessions successfully returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping
    public Page<TrainingSessionResponse> getAllTrainingSessions(Pageable pageable) {
        return service.getAllTrainingSessions(pageable);
    }

    @Operation(
            summary = "Create training session",
            description = "Creates a new training session for a trainer and school service"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Training session successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid session data or trainer does not provide the selected service"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Trainer or service not found"),
            @ApiResponse(responseCode = "409", description = "Trainer already has an overlapping training session")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingSessionResponse createTrainingSession(
            @Valid @RequestBody CreateTrainingSessionRequest request
    ) {
        return service.createTrainingSession(request);
    }

    @Operation(
            summary = "Get current trainer training sessions",
            description = "Returns training sessions belonging to the trainer linked to the authenticated account"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training sessions successfully returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Trainer role required"),
            @ApiResponse(responseCode = "404", description = "Trainer profile not found")
    })
    @GetMapping("/me")
    public Page<TrainingSessionResponse> getCurrentTrainingSessions(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable
    ) {
        return service.getCurrentTrainingSessions(
                jwt.getSubject(),
                pageable
        );
    }

    @Operation(
            summary = "Get training session by ID",
            description = "Returns a training session by its identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training session found"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Training session not found")
    })
    @GetMapping("/{sessionId}")
    public TrainingSessionResponse getTrainingSessionById(@PathVariable Long sessionId) {
        return service.getTrainingSessionById(sessionId);
    }

    @Operation(
            summary = "Update training session",
            description = "Updates an existing training session by its identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training session successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid session data or trainer does not provide the selected service"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Training session, trainer or service not found"),
            @ApiResponse(responseCode = "409", description = "Trainer already has an overlapping training session")
    })
    @PutMapping("/{sessionId}")
    public TrainingSessionResponse  updateTrainingSession(@PathVariable Long sessionId, @Valid @RequestBody UpdateTrainingSessionRequest request){
        return service.updateTrainingSession(sessionId, request);
    }

    @Operation(
            summary = "Delete training session",
            description = "Deletes a training session by its identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Training session successfully deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Training session not found")
    })
    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrainingSession (@PathVariable Long sessionId){
        service.deleteTrainingSession(sessionId);
    }

}
