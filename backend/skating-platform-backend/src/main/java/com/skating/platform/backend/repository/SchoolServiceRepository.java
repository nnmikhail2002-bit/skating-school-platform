package com.skating.platform.backend.repository;

import com.skating.platform.backend.entity.SchoolService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SchoolServiceRepository extends JpaRepository<SchoolService, Long>, JpaSpecificationExecutor <SchoolService> {
    Page<SchoolService> findByNameContainingIgnoreCaseOrTypeContainingIgnoreCase(
            String name,
            String type,
            Pageable pageable
    );
}
