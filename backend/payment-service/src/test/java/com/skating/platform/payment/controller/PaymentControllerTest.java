
package com.skating.platform.payment.controller;

import com.skating.platform.payment.dto.CreatePaymentRequest;
import com.skating.platform.payment.dto.PaymentResponse;
import com.skating.platform.payment.entity.PaymentStatus;
import com.skating.platform.payment.service.PaymentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private PaymentResponse paymentResponse() {
        return new PaymentResponse(
                1L,
                100L,
                new BigDecimal("1500.00"),
                "RUB",
                PaymentStatus.PENDING,
                OffsetDateTime.parse("2026-10-26T17:00:00Z")
        );
    }

    @Test
    void createPayment_validRequest_shouldReturn200() throws Exception {

        when(paymentService.createPayment(any(CreatePaymentRequest.class)))
                .thenReturn(paymentResponse());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 100,
                                  "amount": 1500.00,
                                  "currency": "RUB",
                                  "idempotencyKey": "test-key-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.currency").value("RUB"));
    }

    @Test
    void createPayment_negativeAmount_shouldReturn400() throws Exception {

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 100,
                                  "amount": -500.00,
                                  "currency": "RUB",
                                  "idempotencyKey": "test-key-002"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void createPayment_conflict_shouldReturn409() throws Exception {

        when(paymentService.createPayment(any(CreatePaymentRequest.class)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency key already used"
                ));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 100,
                                  "amount": 2000.00,
                                  "currency": "RUB",
                                  "idempotencyKey": "test-key-001"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void getPayment_existingId_shouldReturn200() throws Exception {

        when(paymentService.getPayment(1L))
                .thenReturn(paymentResponse());

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getPayment_unknownId_shouldReturn404() throws Exception {

        when(paymentService.getPayment(999L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
                ));

        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound());
    }
}
