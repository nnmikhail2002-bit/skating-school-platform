
package com.skating.platform.payment.integration;

import com.jayway.jsonpath.JsonPath;
import com.skating.platform.payment.entity.PaymentTransaction;
import com.skating.platform.payment.repository.PaymentTransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PaymentIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentTransactionRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private String requestJson(long orderId, String amount, String key) {
        return """
                {
                  "orderId": %d,
                  "amount": %s,
                  "currency": "RUB",
                  "idempotencyKey": "%s"
                }
                """.formatted(orderId, amount, key);
    }

    @Test
    void createPayment_shouldSaveToPostgres() throws Exception {

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(100L, "1500.00", "key-001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertEquals(1, repository.count());

        PaymentTransaction saved = repository.findByIdempotencyKey("key-001")
                .orElseThrow();

        assertEquals(100L, saved.getOrderId());
        assertEquals("RUB", saved.getCurrency());
    }

    @Test
    void repeatedRequest_shouldReturnSamePayment() throws Exception {

        String json = requestJson(100L, "1500.00", "key-002");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        Long originalId = repository.findByIdempotencyKey("key-002")
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(originalId.intValue()));

        assertEquals(1, repository.count());
    }

    @Test
    void sameKeyDifferentAmount_shouldReturn409() throws Exception {

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(100L, "1500.00", "key-003")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(100L, "2000.00", "key-003")))
                .andExpect(status().isConflict());

        assertEquals(1, repository.count());
    }

    @Test
    void invalidAmount_shouldReturn400() throws Exception {

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(100L, "-100.00", "key-004")))
                .andExpect(status().isBadRequest());

        assertEquals(0, repository.count());
    }

    @Test
    void unknownPayment_shouldReturn404() throws Exception {

        mockMvc.perform(get("/api/payments/999999"))
                .andExpect(status().isNotFound());
    }

    @RepeatedTest(5)
    void concurrentSameKey_shouldReturnSamePayment() throws Exception {

        int requestCount = 8;

        String json = requestJson(
                200L,
                "1500.00",
                "concurrent-payment-key"
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(requestCount);

        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<MvcResult>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {

                Future<MvcResult> future = executor.submit(() -> {

                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Timeout waiting for concurrent start"
                        );
                    }

                    return mockMvc.perform(post("/api/payments")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                            .andReturn();
                });

                futures.add(future);
            }

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Not all threads are ready"
            );

            start.countDown();

            List<Integer> statusCodes = new ArrayList<>();
            Set<Long> paymentIds = new HashSet<>();

            for (Future<MvcResult> future : futures) {

                MvcResult result = future.get(30, TimeUnit.SECONDS);

                int status = result.getResponse().getStatus();
                statusCodes.add(status);

                if (status == 200) {

                    Number id = JsonPath.read(
                            result.getResponse().getContentAsString(),
                            "$.id"
                    );

                    paymentIds.add(id.longValue());
                }
            }

            assertEquals(
                    1L,
                    repository.count(),
                    "Only one payment should exist in the database"
            );

            assertTrue(
                    statusCodes.stream().allMatch(status -> status == 200),
                    "Unexpected HTTP statuses: " + statusCodes
            );

            assertEquals(
                    1,
                    paymentIds.size(),
                    "All requests should return the same payment ID"
            );

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
