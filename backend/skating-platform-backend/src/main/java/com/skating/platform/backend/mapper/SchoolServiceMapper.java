package com.skating.platform.backend.mapper;

import com.skating.platform.backend.dto.response.SchoolServiceResponse;
import com.skating.platform.backend.entity.SchoolService;

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
}
