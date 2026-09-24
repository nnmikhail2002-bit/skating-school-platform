package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.service.request.CreateSchoolServiceRequest;
import com.skating.platform.backend.dto.service.request.UpdateSchoolServiceRequest;
import com.skating.platform.backend.dto.service.response.SchoolServiceResponse;
import com.skating.platform.backend.entity.SchoolService;
import com.skating.platform.backend.entity.Trainer;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.SchoolServiceMapper;
import com.skating.platform.backend.repository.SchoolServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class SchoolServiceService {
    private final SchoolServiceRepository repository;
    private final SchoolServiceMapper mapper;

    public SchoolServiceService(SchoolServiceRepository repository, SchoolServiceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Page<SchoolServiceResponse> getAllServices(Pageable pageable){
        return repository.findAll(pageable)
                .map(mapper::toResponse); // (mapper -> mapper.toResponse(service))
    }

    public SchoolServiceResponse getServiceById(Long id){
        SchoolService service = getEntityById(id);
        return mapper.toResponse(service);
    }

    public SchoolServiceResponse createService(CreateSchoolServiceRequest request){
        SchoolService service = mapper.toEntity(request);
        SchoolService saved = repository.save(service);
        return mapper.toResponse(saved);
    }

    SchoolService getEntityById(Long id){
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

    @Transactional
    public void deleteService(Long id){
        SchoolService service = getEntityById(id);
        for(Trainer trainer : service.getTrainers()){
            trainer.getServices().remove(service);
        }
        repository.delete(service);
    }

    public Page<SchoolServiceResponse> searchServices(String query, Pageable pageable){
        return repository
                .findByNameContainingIgnoreCaseOrTypeContainingIgnoreCase(query, query, pageable)
                .map(mapper::toResponse);
    }

    public Page<SchoolServiceResponse> filterServices(
            String name,
            String type,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean active,
            Pageable pageable
    ){
        Specification<SchoolService> spec = (root, query, cb) ->
                cb.conjunction();

        if(name != null && !name.isBlank()){
            spec = spec.and(
                    (root, query, cb) ->
                             cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }
        if (type != null && !type.isBlank()) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(cb.lower(root.get("type")), type.toLowerCase())
            );
        }

        if (minPrice != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.greaterThanOrEqualTo(root.get("price"), minPrice)
            );
        }

        if (maxPrice != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.lessThanOrEqualTo(root.get("price"), maxPrice)
            );
        }

        if (active != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(root.get("active"), active)
            );
        }

        return repository.findAll(spec, pageable)
                .map(mapper::toResponse);
    }

}