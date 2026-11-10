
package com.skating.platform.backend.kafka;

import com.skating.platform.backend.dto.payment.PaymentResultEvent;
import com.skating.platform.backend.service.BookingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultListener {

    private final BookingService bookingService;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics =
            {
                    "payments.completed",
                    "payments.failed"
            }
    )
    public void handlePaymentResult(String message) throws JacksonException {

        PaymentResultEvent event = jsonMapper.readValue(message, PaymentResultEvent.class);

        log.info("Received payment result. paymentId={}, orderId={}, status={}",
                event.paymentId(),
                event.orderId(),
                event.status()
        );

        bookingService.processPaymentResult(event);
    }
}
