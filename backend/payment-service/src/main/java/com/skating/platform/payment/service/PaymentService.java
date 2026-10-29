
package com.skating.platform.payment.service;

import com.skating.platform.payment.dto.CreatePaymentRequest;
import com.skating.platform.payment.dto.PaymentResponse;
import com.skating.platform.payment.entity.PaymentStatus;
import com.skating.platform.payment.entity.PaymentTransaction;
import com.skating.platform.payment.repository.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;


@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransactionRepository repository;

    private final JdbcTemplate jdbcTemplate;

    private void lockIdempotencyKey(String key) {

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))"
            )) {

                statement.setString(1, "payment-idempotency:" + key);
                statement.execute();
            }

            return null;
        });
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {

        lockIdempotencyKey(request.idempotencyKey());
        var existing = repository.findByIdempotencyKey(request.idempotencyKey());

        if (existing.isPresent()) {
            PaymentTransaction payment = existing.get();

            if (!payment.getOrderId().equals(request.orderId())
                    || payment.getAmount().compareTo(request.amount()) != 0
                    || !payment.getCurrency().equals(request.currency())) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency key already used for another payment"
                );
            }

            return toResponse(payment);
        }

        PaymentTransaction payment = new PaymentTransaction();

        payment.setOrderId(request.orderId());
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setIdempotencyKey(request.idempotencyKey());
        payment.setStatus(PaymentStatus.PENDING);

        PaymentTransaction saved = repository.saveAndFlush(payment);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {

        PaymentTransaction payment = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
                ));

        return toResponse(payment);
    }

    private PaymentResponse toResponse(PaymentTransaction payment) {

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}
