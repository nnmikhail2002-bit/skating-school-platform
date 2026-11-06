
package com.skating.platform.payment.service;

import com.skating.platform.payment.dto.CreatePaymentRequest;
import com.skating.platform.payment.dto.PaymentResponse;
import com.skating.platform.payment.entity.PaymentStatus;
import com.skating.platform.payment.entity.PaymentTransaction;
import com.skating.platform.payment.gateway.PaymentGateway;
import com.skating.platform.payment.gateway.PaymentGatewayResult;
import com.skating.platform.payment.repository.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransactionRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final PaymentGateway paymentGateway;

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

        Optional<PaymentTransaction> existing =
                repository.findByIdempotencyKey(request.idempotencyKey());

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

        PaymentGatewayResult gatewayResult;

        try {
            gatewayResult = paymentGateway.processPayment(
                    saved.getId(),
                    saved.getAmount(),
                    saved.getCurrency()
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Payment gateway error for paymentId={}. Payment remains PENDING.",
                    saved.getId(),
                    exception
            );

            return toResponse(saved);
        }

        saved.setStatus(gatewayResult.status());

        saved.setGatewayReference(gatewayResult.providerReference());

        PaymentTransaction completed = repository.saveAndFlush(saved);

        return toResponse(completed);
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

    public void reconcilePendingPayments() {

        List<PaymentTransaction> pendingPayments = repository.findByStatusAndGatewayReferenceIsNotNull(PaymentStatus.PENDING);

        for (PaymentTransaction payment : pendingPayments) {

            try {
                PaymentStatus gatewayStatus = paymentGateway.checkPaymentStatus(payment.getGatewayReference());

                if (gatewayStatus == PaymentStatus.PENDING) {
                    continue;
                }

                int updatedRows = repository.updateStatusIfCurrentStatus(
                        payment.getId(),
                        PaymentStatus.PENDING,
                        gatewayStatus
                );

                if (updatedRows == 1) {
                    log.info(
                            "Payment status updated. paymentId={}, status={}",
                            payment.getId(),
                            gatewayStatus
                    );
                }

            } catch (RuntimeException exception) {
                log.error(
                        "Failed to check payment status. paymentId={}",
                        payment.getId(),
                        exception
                );
            }
        }
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
