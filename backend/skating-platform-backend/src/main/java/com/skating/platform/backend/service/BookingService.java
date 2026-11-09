
package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.booking.request.CreateBookingRequest;
import com.skating.platform.backend.dto.booking.response.BookingResponse;
import com.skating.platform.backend.entity.*;
import com.skating.platform.backend.exception.BadRequestException;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.BookingMapper;
import com.skating.platform.backend.repository.*;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class BookingService {

    private final BookingRepository repository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final BookingMapper mapper;
    private final AppUserRepository appUserRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;


    public BookingService(
            BookingMapper mapper,
            TrainingSessionRepository trainingSessionRepository,
            BookingRepository repository,
            AppUserRepository appUserRepository,
            OutboxEventRepository outboxEventRepository,
            JsonMapper jsonMapper
    ) {
        this.mapper = mapper;
        this.repository = repository;
        this.trainingSessionRepository = trainingSessionRepository;
        this.appUserRepository = appUserRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
    }


    private TrainingSession getTrainingSessionByIdForUpdate(Long id) {

        return trainingSessionRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Training session not found"
                        )
                );
    }


    private void validateSession(TrainingSession session) {

        if (session.getStatus() != TrainingSessionStatus.SCHEDULED) {
            throw new BadRequestException(
                    "Training session is not available for booking"
            );
        }
    }


    private void validateCapacity(TrainingSession session) {

        long occupiedCount =
                repository.countByTrainingSession_IdAndStatusIn(
                        session.getId(),
                        List.of(
                                BookingStatus.BOOKED,
                                BookingStatus.PENDING_PAYMENT
                        )
                );

        if (occupiedCount >= session.getCapacity()) {
            throw new ConflictException(
                    "Training session is full"
            );
        }
    }


    @Transactional(readOnly = true)
    public Page<BookingResponse> getCurrentTrainerBookings(
            String userEmail,
            Pageable pageable
    ) {

        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        Trainer trainer = user.getTrainer();

        if (trainer == null) {
            throw new ResourceNotFoundException(
                    "Trainer profile not found"
            );
        }

        return repository
                .findByTrainingSession_Trainer_Id(
                        trainer.getId(),
                        pageable
                )
                .map(mapper::toResponse);
    }


    @Transactional(readOnly = true)
    public Page<BookingResponse> getCurrentBookings(
            String email,
            Pageable pageable
    ) {

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        Student student = user.getStudent();

        if (student == null) {
            throw new ResourceNotFoundException(
                    "Student profile not found"
            );
        }

        Long studentId = student.getId();

        return repository.findByStudent_Id(studentId, pageable)
                .map(mapper::toResponse);
    }


    @Transactional
    public BookingResponse createBooking(
            CreateBookingRequest request,
            String email
    ) {

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        Student student = user.getStudent();

        if (student == null) {
            throw new ResourceNotFoundException(
                    "Student profile not found"
            );
        }

        TrainingSession session =
                getTrainingSessionByIdForUpdate(
                        request.getTrainingSessionId()
                );

        validateSession(session);

        Optional<Booking> existingBooking =
                repository.findByStudent_IdAndTrainingSession_Id(
                        student.getId(),
                        session.getId()
                );


        if (existingBooking.isPresent()) {

            Booking existing = existingBooking.get();

            if (existing.getStatus() == BookingStatus.BOOKED) {
                throw new ConflictException(
                        "Student is already booked for this training session"
                );
            }

            if (existing.getStatus() == BookingStatus.PENDING_PAYMENT) {
                return mapper.toResponse(existing);
            }
        }


        Booking booking;

        if (existingBooking.isPresent()) {

            booking = existingBooking.get();

        } else {

            booking = new Booking();
            booking.setStudent(student);
            booking.setTrainingSession(session);
        }


        validateCapacity(session);


        UUID paymentAttemptId = UUID.randomUUID();

        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setAmount(session.getService().getPrice());
        booking.setCurrency("RUB");
        booking.setPaymentAttemptId(paymentAttemptId);
        booking.setBookedAt(LocalDateTime.now());


        Booking saved = repository.saveAndFlush(booking);

        createPaymentRequestedEvent(saved);

        return mapper.toResponse(saved);
    }


    private void createPaymentRequestedEvent(Booking booking) {

        PaymentRequestedPayload payload = new PaymentRequestedPayload(
                booking.getId(),
                booking.getAmount(),
                booking.getCurrency(),
                booking.getPaymentAttemptId().toString()
        );

        String jsonPayload;

        try {

            jsonPayload = jsonMapper.writeValueAsString(payload);

        } catch (JacksonException exception) {

            throw new IllegalStateException(
                    "Failed to serialize payment request event",
                    exception
            );
        }


        OutboxEvent event = new OutboxEvent();

        event.setAggregateType("BOOKING");
        event.setAggregateId(booking.getId().toString());
        event.setEventType("PAYMENT_REQUESTED");
        event.setPayload(jsonPayload);

        outboxEventRepository.save(event);
    }


    Booking getEntityById(Long id) {

        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found")
                );
    }


    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(Pageable pageable) {

        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }


    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {

        Booking booking = getEntityById(id);

        return mapper.toResponse(booking);
    }


    @Transactional
    public BookingResponse cancelBooking(
            CreateBookingRequest request,
            String email
    ) {

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        Student student = user.getStudent();

        if (student == null) {
            throw new ResourceNotFoundException(
                    "Student profile not found"
            );
        }

        Booking booking =
                repository.findByStudent_IdAndTrainingSession_Id(
                        student.getId(),
                        request.getTrainingSessionId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found")
                );


        getTrainingSessionByIdForUpdate(
                booking.getTrainingSession().getId()
        );


        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return mapper.toResponse(booking);
        }

        booking.setStatus(BookingStatus.CANCELLED);

        Booking saved = repository.save(booking);

        return mapper.toResponse(saved);
    }


    private record PaymentRequestedPayload(
            Long orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {
    }
}
