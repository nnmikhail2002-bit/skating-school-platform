
package com.skating.platform.backend.outbox;

import com.skating.platform.backend.entity.OutboxEvent;
import com.skating.platform.backend.repository.OutboxEventRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OutboxEventPublisherTest {

    private OutboxEventRepository repository;

    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxEventPublisher publisher;



    private OutboxEvent createEvent(
            String eventType,
            String aggregateId
    ) {

        OutboxEvent event = new OutboxEvent();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        event.setId(UUID.randomUUID());

        event.setAggregateType("BOOKING");

        event.setAggregateId(aggregateId);

        event.setEventType(eventType);

        event.setPayload(
                "{\"bookingId\":\"" + aggregateId + "\"}"
        );

        event.setCreatedAt(now);

        event.setNextAttemptAt(now);

        event.setAttempts(0);

        return event;
    }


    private CompletableFuture<SendResult<String, String>> successfulFuture() {

        return CompletableFuture.completedFuture(null);
    }


    private CompletableFuture<SendResult<String, String>> failedFuture(
            Throwable exception
    ) {

        CompletableFuture<SendResult<String, String>> future =
                new CompletableFuture<>();

        future.completeExceptionally(exception);

        return future;
    }


    private CompletableFuture<SendResult<String, String>> timeoutFuture() {

        return new CompletableFuture<>() {

            @Override
            public SendResult<String, String> get(
                    long timeout,
                    TimeUnit unit
            ) throws InterruptedException,
                    ExecutionException,
                    TimeoutException {

                throw new TimeoutException(
                        "Kafka acknowledgement timed out"
                );
            }
        };
    }


    private CompletableFuture<SendResult<String, String>> interruptedFuture() {

        return new CompletableFuture<>() {

            @Override
            public SendResult<String, String> get(
                    long timeout,
                    TimeUnit unit
            ) throws InterruptedException,
                    ExecutionException,
                    TimeoutException {

                throw new InterruptedException(
                        "Test interruption"
                );
            }
        };
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {

        repository = mock(OutboxEventRepository.class);

        kafkaTemplate = mock(KafkaTemplate.class);

        publisher = new OutboxEventPublisher(repository, kafkaTemplate);
    }


    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }


    @Test
    void publishPendingEvents_shouldPublishEventSuccessfully() {

        OutboxEvent event = createEvent("PAYMENT_REQUESTED", "booking-123");

        event.setLastError("Previous error");

        when(repository.findPendingEventsForUpdate(any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of(event));

        when(kafkaTemplate.send("payments.requested", "booking-123", event.getPayload()))
                .thenReturn(successfulFuture());


        publisher.publishPendingEvents();


        assertNotNull(event.getPublishedAt());

        assertNull(event.getLastError());

        assertEquals(0, event.getAttempts());

        verify(kafkaTemplate).send(
                "payments.requested",
                "booking-123",
                event.getPayload()
        );
    }


    @Test
    void publishPendingEvents_whenNoEvents_shouldDoNothing() {

        when(repository.findPendingEventsForUpdate(any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of());


        publisher.publishPendingEvents();


        verifyNoInteractions(kafkaTemplate);
    }


    @Test
    void publishPendingEvents_whenKafkaFails_shouldScheduleRetry() {

        OutboxEvent event = createEvent("PAYMENT_REQUESTED", "booking-123");

        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

        when(repository.findPendingEventsForUpdate(any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(failedFuture(
                new IllegalStateException("Kafka unavailable")
        ));


        publisher.publishPendingEvents();

        OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);


        assertNull(event.getPublishedAt());

        assertEquals(1, event.getAttempts());

        assertTrue(event.getLastError().contains("Kafka unavailable"));

        assertTrue(event.getNextAttemptAt().isAfter(before.plusSeconds(4)));

        assertTrue(event.getNextAttemptAt().isBefore(after.plusSeconds(6)));
    }

    @Test
    void publishPendingEvents_whenOneEventFails_shouldContinueWithNextEvent() {

        OutboxEvent failedEvent = createEvent("PAYMENT_REQUESTED", "booking-123");

        OutboxEvent successfulEvent = createEvent("PAYMENT_REQUESTED", "booking-456");

        when(repository.findPendingEventsForUpdate(
                any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of(
                failedEvent,
                successfulEvent
        ));

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(failedFuture(new IllegalStateException("Kafka unavailable")),
                successfulFuture()
        );

        publisher.publishPendingEvents();

        assertEquals(1, failedEvent.getAttempts());

        assertNull(failedEvent.getPublishedAt());

        assertNotNull(successfulEvent.getPublishedAt());

        assertEquals(0, successfulEvent.getAttempts());

        verify(kafkaTemplate, times(2)).send(
                anyString(),
                anyString(),
                anyString()
        );
    }


    // 5. Неподдерживаемый тип события

    @Test
    void publishPendingEvents_whenEventTypeUnsupported_shouldRegisterFailure() {

        OutboxEvent event = createEvent("UNKNOWN_EVENT", "booking-123");

        when(repository.findPendingEventsForUpdate(any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of(event));


        publisher.publishPendingEvents();


        assertEquals(1, event.getAttempts());

        assertNull(event.getPublishedAt());

        assertTrue(event.getLastError().contains("Unsupported event type"));

        verifyNoInteractions(kafkaTemplate);
    }


    @Test
    void publishPendingEvents_whenKafkaTimesOut_shouldScheduleRetry() {

        OutboxEvent event = createEvent("PAYMENT_REQUESTED", "booking-123");

        when(repository.findPendingEventsForUpdate(any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(timeoutFuture());

        publisher.publishPendingEvents();


        assertEquals(1, event.getAttempts());

        assertNull(event.getPublishedAt());

        assertTrue(event.getLastError().contains(
                "Kafka acknowledgement timed out"
        ));
    }


    @Test
    void publishPendingEvents_whenInterrupted_shouldRestoreInterruptFlagAndStop() {

        OutboxEvent firstEvent = createEvent("PAYMENT_REQUESTED", "booking-123");

        OutboxEvent secondEvent = createEvent("PAYMENT_REQUESTED", "booking-456");

        when(repository.findPendingEventsForUpdate(any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of(
                firstEvent,
                secondEvent
        ));

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(interruptedFuture());

        publisher.publishPendingEvents();

        assertTrue(Thread.currentThread().isInterrupted());

        assertEquals(1, firstEvent.getAttempts());

        assertEquals(0, secondEvent.getAttempts());

        verify(kafkaTemplate, times(1)).send(
                anyString(),
                anyString(),
                anyString()
        );
    }


    @Test
    void publishPendingEvents_whenExceptionHasNoMessage_shouldUseExceptionClassName() {

        OutboxEvent event = createEvent("PAYMENT_REQUESTED", "booking-123");

        when(repository.findPendingEventsForUpdate(any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException());


        publisher.publishPendingEvents();


        assertEquals(1, event.getAttempts());

        assertEquals("IllegalStateException", event.getLastError());
    }


    @Test
    void publishPendingEvents_whenErrorMessageTooLong_shouldTruncateError() {

        OutboxEvent event = createEvent("PAYMENT_REQUESTED", "booking-123");

        String longMessage = "x".repeat(2500);

        when(repository.findPendingEventsForUpdate(any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException(longMessage));


        publisher.publishPendingEvents();


        assertEquals(2000, event.getLastError().length());
    }

    @Test
    void publishPendingEvents_shouldApplyExponentialBackoffAndMaximumDelay() {

        OutboxEvent event = createEvent("PAYMENT_REQUESTED", "booking-123");

        int[] expectedDelays = {5, 10, 20, 40, 80, 160, 300, 300};

        when(repository.findPendingEventsForUpdate(any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("Kafka unavailable"));


        for (int i = 0; i < expectedDelays.length; i++) {

            OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

            publisher.publishPendingEvents();

            OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

            int expectedDelay = expectedDelays[i];

            assertEquals(i + 1, event.getAttempts());

            assertFalse(event.getNextAttemptAt().isBefore(
                            before.plusSeconds(expectedDelay)
                    )
            );

            assertFalse(event.getNextAttemptAt().isAfter(
                            after.plusSeconds(expectedDelay)
                    )
            );
        }
    }

}
