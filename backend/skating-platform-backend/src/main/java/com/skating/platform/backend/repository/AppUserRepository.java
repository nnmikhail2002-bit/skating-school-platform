package com.skating.platform.backend.repository;

import com.skating.platform.backend.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByEmail(String email);
    Optional<AppUser> findByEmail(String email);
    boolean existsByTrainer_Id(Long trainerId);
    Optional<AppUser> findByTrainer_Id(Long trainerId);
}
