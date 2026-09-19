package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.request.SchoolServiceRequest;
import com.skating.platform.backend.dto.response.SchoolServiceResponse;
import com.skating.platform.backend.service.SchoolServiceService;
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
    public List<SchoolServiceResponse> getAllServices(){
        return schoolServiceService.getAllServices();
    }

    @PostMapping
    public SchoolServiceResponse createService(
            @RequestBody SchoolServiceRequest request
    ){
        return schoolServiceService.createService(request);
    }

    @GetMapping("/{serviceId}")
    public SchoolServiceResponse getServiceById(@PathVariable Long serviceId){
        return schoolServiceService.getServiceById(serviceId);
    }
}
