package com.skating.platform.backend.repository;

import com.skating.platform.backend.entity.Booking;
import com.skating.platform.backend.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    long countByTrainingSession_IdAndStatus(Long trainingSessionId, BookingStatus status);
    Optional<Booking> findByStudent_IdAndTrainingSession_Id(Long studentId, Long trainingSessionId);
}
