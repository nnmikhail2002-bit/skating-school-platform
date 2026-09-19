package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.request.SchoolServiceRequest;
import com.skating.platform.backend.dto.request.UpdateSchoolServiceRequest;
import com.skating.platform.backend.dto.response.SchoolServiceResponse;
import com.skating.platform.backend.entity.SchoolService;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.SchoolServiceMapper;
import com.skating.platform.backend.repository.SchoolServiceRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SchoolServiceService {

    private final SchoolServiceRepository repository;
    private final SchoolServiceMapper mapper;

    public SchoolServiceService(SchoolServiceRepository repository, SchoolServiceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<SchoolServiceResponse> getAllServices(){

        return repository.findAll()
                .stream()
                .map(mapper::toResponse) // (mapper -> mapper.toResponse(service))
                .toList();
    }

    public SchoolServiceResponse getServiceById(Long id){
        SchoolService service;
        service = getEntityById(id);
        return mapper.toResponse(service);
    }

    public SchoolServiceResponse createService(SchoolServiceRequest request){
        SchoolService service = mapper.toEntity(request);
        SchoolService saved = repository.save(service);
        return mapper.toResponse(saved);
    }

    private SchoolService getEntityById(Long id){
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Service not found")
                );
    }

    public SchoolServiceResponse updateService(Long id, UpdateSchoolServiceRequest updatedService){
        SchoolService existing = getEntityById(id);
        existing.setName(updatedService.getName());
        existing.setType(updatedService.getType());
        existing.setPrice(updatedService.getPrice());
        existing.setDescription(updatedService.getDescription());
        existing.setUpdatedAt(OffsetDateTime.now());
        SchoolService saved = repository.save(existing);
        return mapper.toResponse(saved);
    }
    public void deleteService(Long id){
        SchoolService service = getEntityById(id);
        repository.delete(service);
    }
}