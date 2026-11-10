
package com.skating.platform.payment.service;

import com.skating.platform.payment.dto.PaymentEventPayload;
import com.skating.platform.payment.entity.OutboxEvent;
import com.skating.platform.payment.entity.PaymentStatus;
import com.skating.platform.payment.entity.PaymentTransaction;
import com.skating.platform.payment.repository.OutboxEventRepository;
import com.skating.platform.payment.repository.PaymentTransactionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentStatusUpdateServiceTest {

    @Mock
    private PaymentTransactionRepository repository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private PaymentStatusUpdateService service;

    private PaymentTransaction createPayment() {

        PaymentTransaction payment = new PaymentTransaction();

        payment.setId(10L);
        payment.setOrderId(1L);
        payment.setIdempotencyKey("test-key-1");
        payment.setAmount(new BigDecimal("1500.00"));
        payment.setCurrency("RUB");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayReference("mock-payment-10");
        payment.setCreatedAt(OffsetDateTime.parse("2026-11-10T10:00:00+03:00"));

        return payment;
    }

    @Test
    void updateStatusAndPublish_shouldPublishSucceededEvent() {

        PaymentTransaction payment = createPayment();

        when(repository.updateStatusIfCurrentStatus(10L, PaymentStatus.PENDING, PaymentStatus.SUCCEEDED))
                .thenReturn(1);

        doReturn("{\"paymentId\":10}")
                .when(jsonMapper)
                .writeValueAsString(any(PaymentEventPayload.class));

        boolean result = service.updateStatusAndPublish(payment, PaymentStatus.SUCCEEDED);

        assertTrue(result);
        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent event = captor.getValue();

        assertEquals("PAYMENT_COMPLETED", event.getEventType());
        assertEquals("PAYMENT", event.getAggregateType());
        assertEquals("10", event.getAggregateId());
    }

    @Test
    void updateStatusAndPublish_shouldPublishFailedEvent() {

        PaymentTransaction payment = createPayment();

        when(repository.updateStatusIfCurrentStatus(10L, PaymentStatus.PENDING, PaymentStatus.FAILED))
                .thenReturn(1);

        doReturn("{\"paymentId\":10}")
                .when(jsonMapper)
                .writeValueAsString(any(PaymentEventPayload.class));

        boolean result = service.updateStatusAndPublish(payment, PaymentStatus.FAILED);

        assertTrue(result);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository).save(captor.capture());

        assertEquals(
                "PAYMENT_FAILED",
                captor.getValue().getEventType()
        );
    }

    @Test
    void updateStatusAndPublish_shouldNotPublishIfStatusAlreadyChanged() {

        PaymentTransaction payment = createPayment();

        when(repository.updateStatusIfCurrentStatus(10L, PaymentStatus.PENDING, PaymentStatus.SUCCEEDED))
                .thenReturn(0);

        boolean result = service.updateStatusAndPublish(payment, PaymentStatus.SUCCEEDED);

        assertFalse(result);

        assertEquals(PaymentStatus.PENDING, payment.getStatus());

        verifyNoInteractions(jsonMapper, outboxEventRepository);
    }

    @Test
    void updateStatusAndPublish_shouldRejectPendingStatus() {

        PaymentTransaction payment = createPayment();

        assertThrows(IllegalArgumentException.class,
                () -> service.updateStatusAndPublish(
                        payment,
                        PaymentStatus.PENDING
                )
        );

        verifyNoInteractions(repository);
        verifyNoInteractions(outboxEventRepository, jsonMapper);
    }
}
