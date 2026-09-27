package com.skating.platform.backend.repository;

import com.skating.platform.backend.entity.TrainingSession;
import com.skating.platform.backend.entity.TrainingSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {
    @Query("""         
        select count(ts)
        from TrainingSession ts
        where ts.trainer.id = :trainerId
          and ts.status <> :cancelledStatus
          and ts.startTime < :endTime
          and ts.endTime > :startTime
          and (:excludeId is null or ts.id <> :excludeId)
        """) //JPQL
    long countOverlappingSessions(
            @Param("trainerId") Long trainerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId,
            @Param("cancelledStatus") TrainingSessionStatus cancelledStatus
    );
}
