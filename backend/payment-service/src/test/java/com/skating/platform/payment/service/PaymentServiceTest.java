
package com.skating.platform.payment.service;

import com.skating.platform.payment.dto.CreatePaymentRequest;
import com.skating.platform.payment.dto.PaymentResponse;
import com.skating.platform.payment.entity.PaymentStatus;
import com.skating.platform.payment.entity.PaymentTransaction;
import com.skating.platform.payment.gateway.PaymentGateway;
import com.skating.platform.payment.gateway.PaymentGatewayResult;
import com.skating.platform.payment.repository.PaymentTransactionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
import com.skating.platform.payment.repository.OutboxEventRepository;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.util.Optional;
import com.skating.platform.payment.dto.PaymentEventPayload;
import com.skating.platform.payment.entity.OutboxEvent;
import org.mockito.ArgumentCaptor;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository repository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private PaymentStatusUpdateService paymentStatusUpdateService;

    private void stubJsonSerialization() {

        doReturn("{\"paymentId\":10}")
                .when(jsonMapper)
                .writeValueAsString(any(PaymentEventPayload.class));
    }

    @Test
    void createPayment_shouldCreateSucceededPayment() {

        CreatePaymentRequest request = new CreatePaymentRequest(
                1L,
                new BigDecimal("1500.00"),
                "RUB",
                "test-key-1"
        );

        when(repository.findByIdempotencyKey("test-key-1"))
                .thenReturn(Optional.empty());

        when(repository.saveAndFlush(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> {
                    PaymentTransaction payment = invocation.getArgument(0);
                    payment.setId(10L);
                    return payment;
                });

        when(paymentGateway.processPayment(
                10L,
                new BigDecimal("1500.00"),
                "RUB"
        )).thenReturn(new PaymentGatewayResult(
                PaymentStatus.SUCCEEDED,
                "mock-payment-10"
        ));

        stubJsonSerialization();
        PaymentResponse response = paymentService.createPayment(request);

        assertEquals(10L, response.id());
        assertEquals(1L, response.orderId());
        assertEquals(PaymentStatus.SUCCEEDED, response.status());
        assertEquals(new BigDecimal("1500.00"), response.amount());

        verify(repository, times(2))
                .saveAndFlush(any(PaymentTransaction.class));

        verify(paymentGateway).processPayment(
                10L,
                new BigDecimal("1500.00"),
                "RUB"
        );

        ArgumentCaptor<OutboxEvent> eventCaptor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository)
                .save(eventCaptor.capture());

        OutboxEvent event = eventCaptor.getValue();

        assertEquals("PAYMENT_COMPLETED", event.getEventType());
        assertEquals("PAYMENT", event.getAggregateType());
        assertEquals("10", event.getAggregateId());

        assertNotNull(event.getPayload());
        ArgumentCaptor<PaymentEventPayload> payloadCaptor =
                ArgumentCaptor.forClass(PaymentEventPayload.class);

        verify(jsonMapper).writeValueAsString(payloadCaptor.capture());

        assertEquals(
                "test-key-1",
                payloadCaptor.getValue().idempotencyKey()
        );

        assertEquals(
                1L,
                payloadCaptor.getValue().orderId()
        );
    }

    @Test
    void createPayment_gatewayFails_shouldReturnFailedPayment() {

        CreatePaymentRequest request = new CreatePaymentRequest(
                1L,
                new BigDecimal("1500.00"),
                "RUB",
                "test-key-2"
        );

        when(repository.findByIdempotencyKey("test-key-2"))
                .thenReturn(Optional.empty());

        when(repository.saveAndFlush(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> {
                    PaymentTransaction payment = invocation.getArgument(0);
                    payment.setId(11L);
                    return payment;
                });

        when(paymentGateway.processPayment(
                11L,
                new BigDecimal("1500.00"),
                "RUB"
        )).thenReturn(new PaymentGatewayResult(
                PaymentStatus.FAILED,
                null
        ));

        stubJsonSerialization();
        PaymentResponse response = paymentService.createPayment(request);

        assertEquals(11L, response.id());
        assertEquals(PaymentStatus.FAILED, response.status());

        verify(repository, times(2))
                .saveAndFlush(any(PaymentTransaction.class));

        verify(paymentGateway).processPayment(
                11L,
                new BigDecimal("1500.00"),
                "RUB"
        );

        ArgumentCaptor<OutboxEvent> eventCaptor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository)
                .save(eventCaptor.capture());

        OutboxEvent event = eventCaptor.getValue();

        assertEquals("PAYMENT_FAILED", event.getEventType());
        assertEquals("PAYMENT", event.getAggregateType());
        assertEquals("11", event.getAggregateId());

        assertNotNull(event.getPayload());
    }

    @Test
    void createPayment_sameKey_shouldReturnExistingPayment() {

        PaymentTransaction existing = new PaymentTransaction();
        existing.setId(10L);
        existing.setOrderId(1L);
        existing.setAmount(new BigDecimal("1500.00"));
        existing.setCurrency("RUB");
        existing.setStatus(PaymentStatus.PENDING);

        when(repository.findByIdempotencyKey("test-key-1"))
                .thenReturn(Optional.of(existing));

        CreatePaymentRequest request = new CreatePaymentRequest(
                1L,
                new BigDecimal("1500.00"),
                "RUB",
                "test-key-1"
        );

        PaymentResponse response = paymentService.createPayment(request);

        assertEquals(10L, response.id());
        assertEquals(PaymentStatus.PENDING, response.status());

        verify(repository, never())
                .saveAndFlush(any(PaymentTransaction.class));

        verifyNoInteractions(paymentGateway);
    }

    @Test
    void createPayment_sameKeyWithDifferentAmount_shouldThrowConflict() {

        PaymentTransaction existing = new PaymentTransaction();
        existing.setOrderId(1L);
        existing.setAmount(new BigDecimal("1500.00"));
        existing.setCurrency("RUB");

        when(repository.findByIdempotencyKey("test-key-1"))
                .thenReturn(Optional.of(existing));

        CreatePaymentRequest request = new CreatePaymentRequest(
                1L,
                new BigDecimal("2000.00"),
                "RUB",
                "test-key-1"
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> paymentService.createPayment(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(repository, never())
                .saveAndFlush(any(PaymentTransaction.class));

        verifyNoInteractions(paymentGateway);
        verifyNoInteractions(outboxEventRepository, jsonMapper);
    }


    @Test
    void createPayment_gatewayThrows_shouldReturnPendingPayment() {

        CreatePaymentRequest request = new CreatePaymentRequest(
                1L,
                new BigDecimal("1500.00"),
                "RUB",
                "test-key-3"
        );

        when(repository.findByIdempotencyKey("test-key-3"))
                .thenReturn(Optional.empty());

        when(repository.saveAndFlush(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> {
                    PaymentTransaction payment = invocation.getArgument(0);
                    payment.setId(12L);
                    return payment;
                });

        when(paymentGateway.processPayment(
                12L,
                new BigDecimal("1500.00"),
                "RUB"
        )).thenThrow(new IllegalStateException("Gateway timeout"));

        PaymentResponse response = paymentService.createPayment(request);

        assertEquals(12L, response.id());
        assertEquals(PaymentStatus.PENDING, response.status());

        verify(repository, times(1))
                .saveAndFlush(any(PaymentTransaction.class));

        verify(paymentGateway).processPayment(
                12L,
                new BigDecimal("1500.00"),
                "RUB"
        );


        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);

        verifyNoInteractions(outboxEventRepository);
    }


}
