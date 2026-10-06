package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.service.request.CreateSchoolServiceRequest;
import com.skating.platform.backend.dto.service.request.UpdateSchoolServiceRequest;
import com.skating.platform.backend.dto.service.response.SchoolServiceResponse;
import com.skating.platform.backend.service.SchoolServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "Services", description = "Управление услугами школы")

@RestController
@RequestMapping("/api/services")
public class SchoolServiceController {
    private final SchoolServiceService schoolServiceService;

    public SchoolServiceController(SchoolServiceService schoolServiceService){
        this.schoolServiceService = schoolServiceService;
    }

    @Operation(
            summary = "Get all services",
            description = "Returns school services with pagination and sorting"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Services successfully returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping
    public Page<SchoolServiceResponse> getAllServices(Pageable pageable){
        return schoolServiceService.getAllServices(pageable);
    }

    @Operation(
            summary = "Create service",
            description = "Creates a new school service"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Service successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid service data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolServiceResponse createService(
           @Valid @RequestBody CreateSchoolServiceRequest request
    ){
        return schoolServiceService.createService(request);
    }

    @Operation(
            summary = "Get service by ID",
            description = "Returns a school service by its identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service found"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @GetMapping("/{serviceId}")
    public SchoolServiceResponse getServiceById(@PathVariable Long serviceId){
        return schoolServiceService.getServiceById(serviceId);
    }

    @Operation(
            summary = "Update service",
            description = "Updates an existing school service by its identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid service data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @PutMapping("/{serviceId}")
    public SchoolServiceResponse updateService(@PathVariable Long serviceId, @Valid @RequestBody UpdateSchoolServiceRequest request){
        return schoolServiceService.updateService(serviceId, request);
    }

    @Operation(
            summary = "Delete service",
            description = "Deletes a school service by its identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Service successfully deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @DeleteMapping("/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable Long serviceId){
        schoolServiceService.deleteService(serviceId);
    }

    @Operation(
            summary = "Search services",
            description = "Searches services by name or type with pagination and sorting"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/search")
    public Page<SchoolServiceResponse> searchServices(
            @RequestParam String query,
            Pageable pageable
    ){
        return schoolServiceService.searchServices(query, pageable);
    }

    @Operation(
            summary = "Filter services",
            description = "Filters services by name, type, price range and active status with pagination and sorting"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Services successfully filtered"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/filter")
    public Page<SchoolServiceResponse> filterServices(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean active,
            Pageable pageable
    ){
        return schoolServiceService.filterServices(
                name,
                type,
                minPrice,
                maxPrice,
                active,
                pageable
        );
    }

}
