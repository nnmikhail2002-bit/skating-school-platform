package com.skating.platform.backend.mapper;

import com.skating.platform.backend.dto.service.request.CreateSchoolServiceRequest;
import com.skating.platform.backend.dto.service.response.SchoolServiceResponse;
import com.skating.platform.backend.entity.SchoolService;
import org.springframework.stereotype.Component;

@Component
public class SchoolServiceMapper {
    public SchoolServiceResponse toResponse(SchoolService service){
        return new SchoolServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                service.getType(),
                service.getActive()
        );
    }
    public SchoolService toEntity(CreateSchoolServiceRequest request){
        SchoolService schoolService = new SchoolService();
        schoolService.setName(request.getName());
        schoolService.setDescription(request.getDescription());
        schoolService.setPrice(request.getPrice());
        schoolService.setType(request.getType());
        return schoolService;
    }
}
