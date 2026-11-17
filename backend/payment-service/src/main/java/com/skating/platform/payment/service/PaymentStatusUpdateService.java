
package com.skating.platform.payment.service;

import com.skating.platform.payment.dto.PaymentEventPayload;
import com.skating.platform.payment.entity.OutboxEvent;
import com.skating.platform.payment.entity.PaymentStatus;
import com.skating.platform.payment.entity.PaymentTransaction;
import com.skating.platform.payment.repository.OutboxEventRepository;
import com.skating.platform.payment.repository.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class PaymentStatusUpdateService {

    private final PaymentTransactionRepository repository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    @Transactional
    public boolean updateStatusAndPublish(PaymentTransaction payment, PaymentStatus newStatus) {

        String eventType;

        if (newStatus == PaymentStatus.SUCCEEDED) { eventType = "PAYMENT_COMPLETED";
        } else if (newStatus == PaymentStatus.FAILED) { eventType = "PAYMENT_FAILED";
        } else { throw new IllegalArgumentException("Unsupported final payment status: " + newStatus);
        }

        int updatedRows = repository.updateStatusIfCurrentStatus(
                payment.getId(),
                PaymentStatus.PENDING,
                newStatus
        );

        if (updatedRows != 1) {
            return false;
        }

        payment.setStatus(newStatus);

        PaymentEventPayload payload = new PaymentEventPayload(
                payment.getId(),
                payment.getOrderId(),
                payment.getIdempotencyKey(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getGatewayReference(),
                payment.getCreatedAt()
        );

        String jsonPayload;

        try { jsonPayload = jsonMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize payment event", exception);
        }

        OutboxEvent event = new OutboxEvent();

        event.setAggregateType("PAYMENT");
        event.setAggregateId(payment.getId().toString());
        event.setEventType(eventType);
        event.setPayload(jsonPayload);

        outboxEventRepository.save(event);

        return true;
    }
}
