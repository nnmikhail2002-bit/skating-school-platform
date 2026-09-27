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

import java.util.Optional;
import java.time.LocalDateTime;


@Service
public class BookingService {
    private final BookingRepository repository;
    private final StudentRepository studentRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final BookingMapper mapper;

    public BookingService(BookingMapper mapper, TrainingSessionRepository trainingSessionRepository, StudentRepository studentRepository, BookingRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
        this.trainingSessionRepository = trainingSessionRepository;
        this.studentRepository = studentRepository;
    }

    private Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student not found")
                );
    }

    private TrainingSession getTrainingSessionById(Long id) {
        return trainingSessionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Training session not found")
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
        long bookedCount =
                repository.countByTrainingSession_IdAndStatus(
                        session.getId(),
                        BookingStatus.BOOKED
                );
        if (bookedCount >= session.getCapacity()) {
            throw new ConflictException(
                    "Training session is full"
            );
        }
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        Student student = getStudentById(request.getStudentId());
        TrainingSession session = getTrainingSessionById(request.getTrainingSessionId());
        validateSession(session);
        Optional<Booking> existingBooking = repository.findByStudent_IdAndTrainingSession_Id(student.getId(), session.getId());
        if (existingBooking.isPresent() && existingBooking.get().getStatus() == BookingStatus.BOOKED) {
            throw new ConflictException(
                    "Student is already booked for this training session"
            );
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
        booking.setStatus(BookingStatus.BOOKED);
        booking.setBookedAt(LocalDateTime.now());
        Booking saved = repository.save(booking);
        return mapper.toResponse(saved);
    }

    Booking getEntityById(Long id){
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found")
                );
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toResponse); // (mapper -> mapper.toResponse(service))
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = getEntityById(id);
        return mapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long id){
        Booking booking = getEntityById(id);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return mapper.toResponse(booking);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = repository.save(booking);
        return mapper.toResponse(saved);
    }

}
