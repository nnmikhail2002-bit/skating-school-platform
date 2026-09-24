package com.skating.platform.backend.repository;
import com.skating.platform.backend.entity.Trainer;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {
   boolean existsByPhone(String phone);
   Page<Trainer> findDistinctByServicesIsNotEmpty(Pageable pageable);
   Page<Trainer> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
           String firstName,
           String lastName,
           Pageable pageable
   );
}



