package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.service.request.CreateSchoolServiceRequest;
import com.skating.platform.backend.dto.service.request.UpdateSchoolServiceRequest;
import com.skating.platform.backend.dto.service.response.SchoolServiceResponse;

import com.skating.platform.backend.service.SchoolServiceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class SchoolServiceController {
    private final SchoolServiceService schoolServiceService;

    public SchoolServiceController(SchoolServiceService schoolServiceService){
        this.schoolServiceService = schoolServiceService;
    }

    @GetMapping
    public Page<SchoolServiceResponse> getAllServices(Pageable pageable){
        return schoolServiceService.getAllServices(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolServiceResponse createService(
           @Valid @RequestBody CreateSchoolServiceRequest request
    ){
        return schoolServiceService.createService(request);
    }

    @GetMapping("/{serviceId}")
    public SchoolServiceResponse getServiceById(@PathVariable Long serviceId){
        return schoolServiceService.getServiceById(serviceId);
    }

    @PutMapping("/{serviceId}")
    public SchoolServiceResponse updateService(@PathVariable Long serviceId, @Valid @RequestBody UpdateSchoolServiceRequest request){
        return schoolServiceService.updateService(serviceId, request);
    }

    @DeleteMapping("/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable Long serviceId){
        schoolServiceService.deleteService(serviceId);
    }



}
