
package com.skating.platform.payment.outbox;

import com.skating.platform.payment.entity.OutboxEvent;
import com.skating.platform.payment.repository.OutboxEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {

        publisher = new OutboxEventPublisher(repository, kafkaTemplate);

        ReflectionTestUtils.setField(publisher, "batchSize", 50);
    }

    private OutboxEvent createEvent(String eventType) {

        OutboxEvent event = new OutboxEvent();

        event.setId(UUID.randomUUID());
        event.setAggregateType("PAYMENT");
        event.setAggregateId("payment-42");
        event.setEventType(eventType);
        event.setPayload("{\"paymentId\":42}");
        event.setAttempts(0);
        event.setNextAttemptAt(OffsetDateTime.now(ZoneOffset.UTC));

        return event;
    }

    @Test
    void publishPendingEvents_shouldPublishEventSuccessfully() {

        OutboxEvent event = createEvent("PAYMENT_REQUESTED");

        when(repository.findPendingEventsForUpdate(any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send("payments.requested", "payment-42", event.getPayload()))
                .thenReturn(future);

        publisher.publishPendingEvents();

        assertNotNull(event.getPublishedAt());
        assertNull(event.getLastError());
        assertEquals(0, event.getAttempts());

        verify(kafkaTemplate).send("payments.requested", "payment-42", event.getPayload());
    }

    @Test
    void publishPendingEvents_whenKafkaFails_shouldScheduleRetry() {

        OutboxEvent event = createEvent("PAYMENT_REQUESTED");

        when(repository.findPendingEventsForUpdate(any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();

        future.completeExceptionally(new IllegalStateException("broker unavailable"));

        when(kafkaTemplate.send("payments.requested", "payment-42", event.getPayload()))
                .thenReturn(future);

        publisher.publishPendingEvents();

        assertNull(event.getPublishedAt());
        assertEquals(1, event.getAttempts());

        assertTrue(event.getLastError().contains("broker unavailable"));

        assertTrue(event.getNextAttemptAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void publishPendingEvents_whenEventTypeUnsupported_shouldScheduleRetry() {

        OutboxEvent event = createEvent("UNKNOWN_EVENT");

        when(repository.findPendingEventsForUpdate(any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of(event));

        publisher.publishPendingEvents();

        assertEquals(1, event.getAttempts());
        assertNull(event.getPublishedAt());

        assertTrue(event.getLastError().contains("Unsupported event type"));

        verifyNoInteractions(kafkaTemplate);
    }
}
