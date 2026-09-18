package com.skating.platform.backend.repository;

import com.skating.platform.backend.entity.SchoolService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolServiceRepository
        extends JpaRepository<SchoolService, Long> {
}
