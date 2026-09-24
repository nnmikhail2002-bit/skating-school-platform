package com.skating.platform.backend.repository;
import com.skating.platform.backend.entity.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {
   boolean existsByPhone(String phone);
}



