package com.skating.platform.backend.service;

import com.skating.platform.backend.entity.SchoolService;
import com.skating.platform.backend.repository.SchoolServiceRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SchoolServiceService {

    private final SchoolServiceRepository repository;

    public SchoolServiceService(SchoolServiceRepository repository) {
        this.repository = repository;
    }

    public List<SchoolService> getAllServices(){
        return repository.findAll();
    }

    public SchoolService getServiceById(Long id){
        return repository.findById(id)
                .orElseThrow(() ->
        new RuntimeException("Service not found")
                );
    }

    public SchoolService createService(SchoolService service){
        return repository.save(service);
    }

    public SchoolService updateService(Long id, SchoolService updatedService){
        SchoolService existing = getServiceById(id);
        existing.setName(updatedService.getName());
        existing.setActive(updatedService.getActive());
        existing.setType(updatedService.getType());
        existing.setPrice(updatedService.getPrice());
        existing.setDescription(updatedService.getDescription());
        existing.setUpdatedAt(OffsetDateTime.now());
        return repository.save(existing);
    }
    public void deleteService(Long id){
        SchoolService service = getServiceById(id);
        repository.delete(service);
    }
}