
package com.skating.platform.backend.repository;

import com.skating.platform.backend.entity.Booking;
import com.skating.platform.backend.entity.BookingStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    long countByTrainingSession_IdAndStatus(
            Long trainingSessionId,
            BookingStatus status
    );

    long countByTrainingSession_IdAndStatusIn(
            Long trainingSessionId,
            List<BookingStatus> statuses
    );

    Optional<Booking> findByStudent_IdAndTrainingSession_Id(
            Long studentId,
            Long trainingSessionId
    );

    Page<Booking> findByStudent_Id(Long student_Id, Pageable pageable);

    Page<Booking> findByTrainingSession_Trainer_Id(
            Long trainerId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
