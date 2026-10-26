
package com.skating.platform.payment.service;

import com.skating.platform.payment.dto.CreatePaymentRequest;
import com.skating.platform.payment.dto.PaymentResponse;
import com.skating.platform.payment.entity.PaymentStatus;
import com.skating.platform.payment.entity.PaymentTransaction;
import com.skating.platform.payment.repository.PaymentTransactionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository repository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPayment_shouldCreatePendingPayment() {
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

        PaymentResponse response = paymentService.createPayment(request);

        assertEquals(10L, response.id());
        assertEquals(1L, response.orderId());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals(new BigDecimal("1500.00"), response.amount());

        verify(repository).saveAndFlush(any(PaymentTransaction.class));
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

        verify(repository, never()).saveAndFlush(any(PaymentTransaction.class));
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

        verify(repository, never()).saveAndFlush(any(PaymentTransaction.class));
    }
}
