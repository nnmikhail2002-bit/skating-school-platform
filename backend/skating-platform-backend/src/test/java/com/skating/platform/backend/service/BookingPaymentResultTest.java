
package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.payment.PaymentResultEvent;
import com.skating.platform.backend.entity.Booking;
import com.skating.platform.backend.entity.BookingStatus;
import com.skating.platform.backend.mapper.BookingMapper;
import com.skating.platform.backend.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingPaymentResultTest {

    @Mock
    private BookingRepository repository;

    @Mock
    private TrainingSessionRepository trainingSessionRepository;

    @Mock
    private BookingMapper mapper;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {

        bookingService = new BookingService(
                mapper,
                trainingSessionRepository,
                repository,
                appUserRepository,
                outboxEventRepository,
                JsonMapper.builder().build()
        );
    }

    private Booking createBooking(UUID attemptId, BookingStatus status) {

        Booking booking = new Booking();

        booking.setId(100L);
        booking.setPaymentAttemptId(attemptId);
        booking.setStatus(status);
        booking.setAmount(new BigDecimal("2500.00"));
        booking.setCurrency("RUB");

        return booking;
    }

    private PaymentResultEvent createEvent(UUID attemptId, String status) {

        return new PaymentResultEvent(
                500L,
                100L,
                attemptId.toString(),
                new BigDecimal("2500.00"),
                "RUB",
                status,
                "gateway-reference",
                OffsetDateTime.now()
        );
    }

    @Test
    void shouldBookWhenPaymentSucceeded() {

        UUID attemptId = UUID.randomUUID();

        Booking booking = createBooking(attemptId, BookingStatus.PENDING_PAYMENT);

        when(repository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(booking));

        bookingService.processPaymentResult(createEvent(attemptId, "SUCCEEDED"));

        assertEquals(
                BookingStatus.BOOKED,
                booking.getStatus()
        );

        verify(repository).save(booking);
    }

    @Test
    void shouldMarkPaymentFailed() {

        UUID attemptId = UUID.randomUUID();

        Booking booking = createBooking(attemptId, BookingStatus.PENDING_PAYMENT);

        when(repository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(booking));

        bookingService.processPaymentResult(createEvent(attemptId, "FAILED"));

        assertEquals(
                BookingStatus.PAYMENT_FAILED,
                booking.getStatus()
        );

        verify(repository).save(booking);
    }

    @Test
    void shouldIgnoreDuplicatePaymentEvent() {

        UUID attemptId = UUID.randomUUID();

        Booking booking = createBooking(attemptId, BookingStatus.BOOKED);

        when(repository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(booking));

        bookingService.processPaymentResult(createEvent(attemptId, "SUCCEEDED"));

        assertEquals(
                BookingStatus.BOOKED,
                booking.getStatus()
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldIgnoreStalePaymentAttempt() {

        UUID currentAttempt = UUID.randomUUID();
        UUID oldAttempt = UUID.randomUUID();

        Booking booking = createBooking(currentAttempt, BookingStatus.PENDING_PAYMENT);

        when(repository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(booking));

        bookingService.processPaymentResult(createEvent(oldAttempt, "SUCCEEDED"));

        assertEquals(
                BookingStatus.PENDING_PAYMENT,
                booking.getStatus()
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldIgnorePaymentWhenBookingNotFound() {

        UUID attemptId = UUID.randomUUID();

        when(repository.findByIdForUpdate(100L))
                .thenReturn(Optional.empty());

        bookingService.processPaymentResult(createEvent(attemptId, "SUCCEEDED"));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldNotBookCancelledBooking() {

        UUID attemptId = UUID.randomUUID();

        Booking booking = createBooking(attemptId, BookingStatus.CANCELLED);

        when(repository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(booking));

        bookingService.processPaymentResult(createEvent(attemptId, "SUCCEEDED"));

        assertEquals(
                BookingStatus.CANCELLED,
                booking.getStatus()
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectPaymentWithDifferentAmount() {

        UUID attemptId = UUID.randomUUID();

        Booking booking = createBooking(attemptId, BookingStatus.PENDING_PAYMENT);

        when(repository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(booking));

        PaymentResultEvent event = new PaymentResultEvent(
                500L,
                100L,
                attemptId.toString(),
                new BigDecimal("999.00"),
                "RUB",
                "SUCCEEDED",
                "gateway-reference",
                OffsetDateTime.now()
        );

        assertThrows(IllegalStateException.class, () -> bookingService.processPaymentResult(event));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectUnsupportedPaymentStatus() {

        UUID attemptId = UUID.randomUUID();

        Booking booking = createBooking(attemptId, BookingStatus.PENDING_PAYMENT);

        when(repository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, () -> bookingService.processPaymentResult(createEvent(attemptId, "UNKNOWN")));

        verify(repository, never()).save(any());
    }
}
