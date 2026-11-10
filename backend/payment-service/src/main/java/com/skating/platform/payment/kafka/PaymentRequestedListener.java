
package com.skating.platform.payment.kafka;

import com.skating.platform.payment.dto.CreatePaymentRequest;
import com.skating.platform.payment.dto.PaymentRequestedEvent;
import com.skating.platform.payment.dto.PaymentResponse;
import com.skating.platform.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;


@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestedListener {

    private final JsonMapper jsonMapper;
    private final PaymentService paymentService;


    @KafkaListener(
            topics = "payments.requested",
            groupId = "skating-platform-payment-service"
    )
    public void handlePaymentRequested(String message) {

        log.info("Received payment request: {}", message);

        PaymentRequestedEvent event;

        try {

            event = jsonMapper.readValue(
                    message,
                    PaymentRequestedEvent.class
            );

        } catch (JacksonException exception) {

            log.error(
                    "Failed to deserialize payment request",
                    exception
            );

            throw new IllegalArgumentException(
                    "Invalid payment request event",
                    exception
            );
        }


        CreatePaymentRequest request = new CreatePaymentRequest(
                event.orderId(),
                event.amount(),
                event.currency(),
                event.idempotencyKey()
        );


        PaymentResponse response = paymentService.createPayment(request);


        log.info(
                "Payment processed. orderId={}, paymentId={}, status={}",
                response.orderId(),
                response.id(),
                response.status()
        );
    }
}
