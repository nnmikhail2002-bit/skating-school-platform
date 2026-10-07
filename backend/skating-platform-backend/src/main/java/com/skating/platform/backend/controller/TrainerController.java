package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.appusers.request.CreateTrainerAccountRequest;
import com.skating.platform.backend.dto.appusers.response.AppUserResponse;
import com.skating.platform.backend.dto.trainer.request.CreateTrainerRequest;
import com.skating.platform.backend.dto.trainer.response.TrainerResponse;
import com.skating.platform.backend.dto.trainer.request.UpdateTrainerRequest;
import com.skating.platform.backend.dto.trainerService.response.TrainerServiceResponse;
import com.skating.platform.backend.service.AuthService;
import com.skating.platform.backend.service.TrainerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Trainers", description = "Управление тренерами и их услугами")

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {
    private final TrainerService service;
    private final AuthService authService;
    public TrainerController(TrainerService service, AuthService authService){
        this.service = service;
        this.authService = authService;
    }

    @Operation(
            summary = "Create trainer account",
            description = "Creates an application account linked to an existing trainer profile"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainer account successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid account data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Trainer not found"),
            @ApiResponse(responseCode = "409", description = "Email is already in use or trainer already has an account")
    })
    @PostMapping("/{trainerId}/account")
    @ResponseStatus(HttpStatus.CREATED)
    public AppUserResponse createTrainerAccount(
            @PathVariable Long trainerId,
            @Valid @RequestBody CreateTrainerAccountRequest request
    ) {
        return authService.createTrainerAccount(
                trainerId,
                request
        );
    }


    @Operation(
            summary = "Get all trainers",
            description = "Returns trainers with pagination and sorting"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainers successfully returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping
    public Page<TrainerResponse> getAllTrainers(Pageable pageable){
        return service.getAllTrainers(pageable);
    }

    @Operation(
            summary = "Create trainer",
            description = "Creates a new trainer profile"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainer successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid trainer data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "409", description = "Trainer with this phone number already exists")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainerResponse createTrainer (
            @Valid @RequestBody CreateTrainerRequest request
    ){
       return service.createTrainer(request);
    }

    @Operation(
            summary = "Get trainer by ID",
            description = "Returns a trainer by its identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer found"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Trainer not found")
    })
    @GetMapping("/{trainerId}")
    public TrainerResponse getTrainerById (@PathVariable Long trainerId){
        return service.getTrainerById(trainerId);
    }

    @Operation(
            summary = "Get current trainer profile",
            description = "Returns the trainer profile linked to the authenticated account"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current trainer profile returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Trainer role required"),
            @ApiResponse(responseCode = "404", description = "Trainer profile not found")
    })
    @GetMapping("/me")
    public TrainerResponse getCurrentTrainer(@AuthenticationPrincipal Jwt jwt) {
        return service.getCurrentTrainer(jwt.getSubject());
    }

    @Operation(
            summary = "Update trainer",
            description = "Updates an existing trainer by its identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid trainer data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Trainer not found"),
            @ApiResponse(responseCode = "409", description = "Phone number is already used by another trainer")
    })
    @PutMapping("/{trainerId}")
    public TrainerResponse updateTrainer(@PathVariable Long trainerId, @Valid @RequestBody UpdateTrainerRequest request){
        return service.updateTrainer(trainerId, request);
    }

    @Operation(
            summary = "Delete trainer",
            description = "Deletes a trainer by its identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Trainer successfully deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Trainer not found")
    })
    @DeleteMapping("/{trainerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrainer (@PathVariable Long trainerId){
        service.deleteTrainer(trainerId);
    }

    @Operation(
            summary = "Assign service to trainer",
            description = "Assigns a school service to a trainer using trainer and service identifiers"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Service successfully assigned to trainer"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Trainer or service not found")
    })
    @PostMapping("/{trainerId}/services/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addServiceToTrainer (@PathVariable Long trainerId, @PathVariable Long serviceId){
        service.addServiceToTrainer(trainerId, serviceId);
    }

    @Operation(
            summary = "Get services for all trainers",
            description = "Returns trainers together with their assigned services using pagination"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer services successfully returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/services")
    public Page<TrainerServiceResponse> getAllTrainersServices (Pageable pageable){
        return service.getAllTrainersServices(pageable);
    }

    @Operation(
            summary = "Get current trainer services",
            description = "Returns services assigned to the trainer linked to the authenticated account"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current trainer services returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Trainer role required"),
            @ApiResponse(responseCode = "404", description = "Trainer profile not found")
    })
    @GetMapping("/me/services")
    public TrainerServiceResponse getCurrentTrainerServices(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.getCurrentTrainerServices(
                jwt.getSubject()
        );
    }

    @Operation(
            summary = "Get trainer services by trainer ID",
            description = "Returns services assigned to a trainer by trainer identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer services returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Trainer not found")
    })
    @GetMapping("/{trainerId}/services")
    public TrainerServiceResponse getTrainerServicesById (@PathVariable Long trainerId){
        return service.getTrainerServiceById(trainerId);
    }


    @Operation(
            summary = "Get trainers with assigned services",
            description = "Returns trainers that have at least one assigned service"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainers successfully returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/with-services")
    public Page<TrainerServiceResponse> getAllTrainersWithServices (Pageable pageable){
        return service.getAllTrainersWithServices(pageable);
    }
    @Operation(
            summary = "Remove service from trainer",
            description = "Removes an assigned service from a trainer using trainer and service identifiers"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Service successfully removed from trainer"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Trainer or service not found")
    })
    @DeleteMapping("/{trainerId}/services/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrainerService (@PathVariable Long trainerId, @PathVariable Long serviceId){
        service.deleteTrainerService(trainerId, serviceId);
    }

    @Operation(
            summary = "Search trainers",
            description = "Searches trainers by first or last name with pagination and sorting"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/search")
    public Page<TrainerResponse> searchTrainers(
            @RequestParam String query,
            Pageable pageable
    ){
        return service.searchTrainers(query, pageable);
    }
}
