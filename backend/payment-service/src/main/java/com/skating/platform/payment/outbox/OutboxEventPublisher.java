
package com.skating.platform.payment.outbox;

import com.skating.platform.payment.entity.OutboxEvent;
import com.skating.platform.payment.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final int SEND_TIMEOUT_SECONDS = 10;
    private static final int MAX_ERROR_LENGTH = 2000;

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${payment.outbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${payment.outbox.fixed-delay:1000}")
    @Transactional
    public void publishPendingEvents() {

        List<OutboxEvent> events = repository.findPendingEventsForUpdate(OffsetDateTime.now(ZoneOffset.UTC), batchSize);

        for (OutboxEvent event : events) {
            try {
                publishEvent(event);
            } catch (InterruptedException exception) {

                Thread.currentThread().interrupt();
                registerFailure(event, exception);
                break;
            } catch (Exception exception) {
                registerFailure(event, exception);
            }
        }
    }

    private void publishEvent(OutboxEvent event) throws Exception {

        String topic = resolveTopic(event.getEventType());

        kafkaTemplate.send(
                topic,
                event.getAggregateId(),
                event.getPayload()
        ).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        event.setPublishedAt(OffsetDateTime.now(ZoneOffset.UTC));
        event.setLastError(null);

        log.info(
                "Outbox event published. eventId={}, eventType={}, topic={}",
                event.getId(),
                event.getEventType(),
                topic
        );
    }

    private String resolveTopic(String eventType) {

        return switch (eventType) {

            case "PAYMENT_REQUESTED" -> "payments.requested";

            case "PAYMENT_COMPLETED" -> "payments.completed";

            case "PAYMENT_FAILED" -> "payments.failed";

            default -> throw new IllegalArgumentException(
                    "Unsupported event type: " + eventType
            );
        };
    }

    private void registerFailure(OutboxEvent event, Exception exception) {

        int attempts = event.getAttempts() + 1;

        event.setAttempts(attempts);

        long retryDelay = calculateRetryDelay(attempts);

        event.setNextAttemptAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(retryDelay));

        String errorMessage = exception.getMessage();

        if (errorMessage == null || errorMessage.isBlank()) {
            errorMessage = exception.getClass().getSimpleName();
        }

        if (errorMessage.length() > MAX_ERROR_LENGTH) {
            errorMessage = errorMessage.substring(0, MAX_ERROR_LENGTH);
        }

        event.setLastError(errorMessage);

        log.error(
                "Failed to publish outbox event. eventId={}, attempts={}",
                event.getId(),
                attempts,
                exception
        );
    }

    private long calculateRetryDelay(int attempts) {

        long delay = 5L * (1L << Math.min(attempts - 1, 6));

        return Math.min(delay, 300L);
    }
}
