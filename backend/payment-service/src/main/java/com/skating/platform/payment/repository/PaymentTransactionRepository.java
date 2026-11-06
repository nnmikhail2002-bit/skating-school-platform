
package com.skating.platform.payment.repository;

import com.skating.platform.payment.entity.PaymentStatus;
import com.skating.platform.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
    List<PaymentTransaction> findByOrderId(Long orderId);
    List<PaymentTransaction> findByStatusAndGatewayReferenceIsNotNull(PaymentStatus status);
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
        UPDATE PaymentTransaction p
        SET p.status = :newStatus
        WHERE p.id = :paymentId
          AND p.status = :currentStatus
        """)
    int updateStatusIfCurrentStatus(
            @Param("paymentId") Long paymentId,
            @Param("currentStatus") PaymentStatus currentStatus,
            @Param("newStatus") PaymentStatus newStatus
    );
}
