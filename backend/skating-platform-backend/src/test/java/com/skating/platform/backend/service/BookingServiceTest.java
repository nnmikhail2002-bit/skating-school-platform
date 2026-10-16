package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.booking.request.CreateBookingRequest;
import com.skating.platform.backend.dto.booking.response.BookingResponse;
import com.skating.platform.backend.entity.*;
import com.skating.platform.backend.exception.BadRequestException;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.BookingMapper;
import com.skating.platform.backend.repository.AppUserRepository;
import com.skating.platform.backend.repository.BookingRepository;
import com.skating.platform.backend.repository.TrainingSessionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository repository;

    @Mock
    private TrainingSessionRepository trainingSessionRepository;

    @Mock
    private BookingMapper mapper;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private BookingService bookingService;


    private Student student() {
        Student student = new Student();
        student.setId(1L);
        student.setFirstName("Student");
        student.setLastName("Test");
        student.setPhone("89999999999");
        student.setActive(true);

        return student;
    }


    private Trainer trainer() {
        Trainer trainer = new Trainer();
        trainer.setId(2L);
        trainer.setFirstName("Trainer");
        trainer.setLastName("Test");
        trainer.setPhone("89999999998");
        trainer.setActive(true);

        return trainer;
    }


    private AppUser studentUser(String email, Student student) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setStudent(student);

        return user;
    }


    private AppUser trainerUser(String email, Trainer trainer) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setTrainer(trainer);

        return user;
    }


    private TrainingSession scheduledSession() {
        TrainingSession session = new TrainingSession();

        session.setId(10L);
        session.setStatus(TrainingSessionStatus.SCHEDULED);
        session.setCapacity(2);
        session.setStartTime(LocalDateTime.of(2026, 10, 20, 15, 0));
        session.setEndTime(LocalDateTime.of(2026, 10, 20, 16, 0));

        return session;
    }


    private CreateBookingRequest request() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setTrainingSessionId(10L);
        return request;
    }


    private BookingResponse response(
            BookingStatus status
    ) {

        return new BookingResponse(
                100L,
                1L,
                10L,
                LocalDateTime.of(
                        2026, 10, 20, 14, 0
                ),
                status
        );
    }


    // CREATE BOOKING //


    @Test
    void shouldCreateBookingSuccessfully() {

        String email = "student@test.com";

        Student student = student();
        AppUser user = studentUser(email, student);
        TrainingSession session = scheduledSession();

        CreateBookingRequest request = request();

        BookingResponse response = response(BookingStatus.BOOKED);


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(trainingSessionRepository.findById(10L))
                .thenReturn(Optional.of(session));

        when(repository
                .findByStudent_IdAndTrainingSession_Id(1L, 10L))
                .thenReturn(Optional.empty());

        when(repository
                .countByTrainingSession_IdAndStatus(10L, BookingStatus.BOOKED))
                .thenReturn(1L);

        when(repository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(mapper.toResponse(any(Booking.class)))
                .thenReturn(response);


        BookingResponse result =
                bookingService.createBooking(request, email);


        ArgumentCaptor<Booking> captor =
                ArgumentCaptor.forClass(Booking.class);

        verify(repository).save(captor.capture());

        Booking savedBooking = captor.getValue();


        assertEquals(100L, result.getId());
        assertEquals(BookingStatus.BOOKED, result.getStatus());

        assertEquals(student, savedBooking.getStudent());

        assertEquals(session, savedBooking.getTrainingSession());

        assertEquals(BookingStatus.BOOKED, savedBooking.getStatus());

        assertNotNull(savedBooking.getBookedAt());

        verify(repository)
                .countByTrainingSession_IdAndStatus(10L, BookingStatus.BOOKED);
    }


    @Test
    void shouldRestoreCancelledBookingSuccessfully() {

        String email = "student@test.com";

        Student student = student();

        AppUser user = studentUser(email, student);

        TrainingSession session = scheduledSession();

        Booking existing = new Booking();

        existing.setId(100L);
        existing.setStudent(student);
        existing.setTrainingSession(session);
        existing.setStatus(BookingStatus.CANCELLED);

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(trainingSessionRepository.findById(10L))
                .thenReturn(Optional.of(session));

        when(repository
                .findByStudent_IdAndTrainingSession_Id(1L, 10L))
                .thenReturn(Optional.of(existing));

        when(repository.countByTrainingSession_IdAndStatus(10L, BookingStatus.BOOKED))
                .thenReturn(0L);

        when(repository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(mapper.toResponse(any(Booking.class)))
                .thenReturn(response(BookingStatus.BOOKED));


        bookingService.createBooking(request(), email);


        ArgumentCaptor<Booking> captor =
                ArgumentCaptor.forClass(
                        Booking.class
                );

        verify(repository).save(captor.capture());

        Booking saved = captor.getValue();


        assertSame(existing, saved);

        assertEquals(
                BookingStatus.BOOKED,
                saved.getStatus()
        );

        assertNotNull(saved.getBookedAt());
    }


    @Test
    void shouldRejectWhenStudentAlreadyBooked() {

        String email = "student@test.com";

        Student student = student();

        AppUser user = studentUser(email, student);

        TrainingSession session = scheduledSession();

        Booking existing = new Booking();

        existing.setStudent(student);
        existing.setTrainingSession(session);
        existing.setStatus(BookingStatus.BOOKED);


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(trainingSessionRepository.findById(10L))
                .thenReturn(Optional.of(session));

        when(repository
                .findByStudent_IdAndTrainingSession_Id(1L, 10L))
                .thenReturn(Optional.of(existing));


        ConflictException exception =
                assertThrows(ConflictException.class,
                        () -> bookingService.createBooking(request(), email)
                );


        assertEquals(
                "Student is already booked for this training session",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(Booking.class));
    }


    @Test
    void shouldRejectCreateWhenTrainingSessionIsNotScheduled() {

        String email = "student@test.com";

        Student student = student();

        AppUser user = studentUser(email, student);

        TrainingSession session = scheduledSession();

        session.setStatus(
                TrainingSessionStatus.CANCELLED
        );


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(trainingSessionRepository.findById(10L))
                .thenReturn(Optional.of(session));


        BadRequestException exception =
                assertThrows(BadRequestException.class,
                        () -> bookingService.createBooking(request(), email)
                );


        assertEquals(
                "Training session is not available for booking",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(Booking.class));
    }


    @Test
    void shouldRejectCreateWhenTrainingSessionIsFull() {

        String email = "student@test.com";

        Student student = student();

        AppUser user = studentUser(email, student);

        TrainingSession session =
                scheduledSession();


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(trainingSessionRepository.findById(10L))
                .thenReturn(Optional.of(session));

        when(repository.findByStudent_IdAndTrainingSession_Id(1L, 10L))
                .thenReturn(Optional.empty());

        when(repository
                .countByTrainingSession_IdAndStatus(10L, BookingStatus.BOOKED))
                .thenReturn(2L);


        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> bookingService.createBooking(request(), email)
                );


        assertEquals(
                "Training session is full",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(Booking.class));
    }


    @Test
    void shouldRejectCreateWhenUserNotFound() {

        String email = "missing@test.com";

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> bookingService.createBooking(request(), email)
                );


        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verifyNoInteractions(
                trainingSessionRepository
        );
    }


    @Test
    void shouldRejectCreateWhenStudentProfileNotFound() {

        String email = "trainer@test.com";

        AppUser user = new AppUser();

        user.setEmail(email);
        user.setStudent(null);


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> bookingService.createBooking(request(), email)
                );


        assertEquals(
                "Student profile not found",
                exception.getMessage()
        );

        verifyNoInteractions(
                trainingSessionRepository
        );
    }


    @Test
    void shouldRejectCreateWhenTrainingSessionNotFound() {

        String email = "student@test.com";

        Student student = student();

        AppUser user = studentUser(email, student);


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(trainingSessionRepository.findById(10L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> bookingService.createBooking(request(), email)
                );


        assertEquals(
                "Training session not found",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(Booking.class));
    }


    // CURRENT STUDENT BOOKINGS //

    @Test
    void shouldGetCurrentBookingsSuccessfully() {

        String email = "student@test.com";

        Pageable pageable = PageRequest.of(0, 10);

        Student student = student();

        AppUser user = studentUser(email, student);

        Booking booking =
                new Booking();

        booking.setId(100L);

        Page<Booking> bookings =
                new PageImpl<>(List.of(booking), pageable, 1);

        BookingResponse response =
                response(BookingStatus.BOOKED);


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(repository.findByStudent_Id(1L, pageable))
                .thenReturn(bookings);

        when(mapper.toResponse(booking))
                .thenReturn(response);


        Page<BookingResponse> result = bookingService.getCurrentBookings(email, pageable);


        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(100L, result.getContent()
                .get(0)
                .getId()
        );

        verify(repository)
                .findByStudent_Id(
                        1L,
                        pageable
                );
    }


    @Test
    void shouldRejectCurrentBookingsWhenUserNotFound() {

        String email = "missing@test.com";

        Pageable pageable = PageRequest.of(0, 10);

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> bookingService
                                .getCurrentBookings(email, pageable)
                );


        assertEquals(
                "User not found",
                exception.getMessage()
        );
    }


    @Test
    void shouldRejectCurrentBookingsWhenStudentProfileNotFound() {

        String email = "trainer@test.com";

        Pageable pageable = PageRequest.of(0, 10);

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setStudent(null);


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> bookingService
                                .getCurrentBookings(email, pageable)
                );


        assertEquals(
                "Student profile not found",
                exception.getMessage()
        );
    }


    // CURRENT TRAINER BOOKINGS //

    @Test
    void shouldGetCurrentTrainerBookingsSuccessfully() {

        String email = "trainer@test.com";

        Pageable pageable = PageRequest.of(0, 10);

        Trainer trainer = trainer();

        AppUser user = trainerUser(email, trainer);

        Booking booking = new Booking();

        booking.setId(100L);

        Page<Booking> bookings =
                new PageImpl<>(
                        List.of(booking),
                        pageable,
                        1
                );


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(repository
                .findByTrainingSession_Trainer_Id(
                        2L,
                        pageable
                ))
                .thenReturn(bookings);

        when(mapper.toResponse(booking))
                .thenReturn(response(BookingStatus.BOOKED));


        Page<BookingResponse> result =
                bookingService
                        .getCurrentTrainerBookings(email, pageable);


        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(100L, result.getContent()
                .get(0)
                .getId()
        );

        verify(repository)
                .findByTrainingSession_Trainer_Id(
                        2L,
                        pageable
                );
    }


    @Test
    void shouldRejectTrainerBookingsWhenUserNotFound() {

        String email = "missing@test.com";

        Pageable pageable = PageRequest.of(0, 10);

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> bookingService
                                .getCurrentTrainerBookings(email, pageable)
                );


        assertEquals(
                "User not found",
                exception.getMessage()
        );
    }


    @Test
    void shouldRejectTrainerBookingsWhenTrainerProfileNotFound() {

        String email = "student@test.com";

        Pageable pageable = PageRequest.of(0, 10);

        AppUser user = new AppUser();

        user.setEmail(email);
        user.setTrainer(null);


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> bookingService.getCurrentTrainerBookings(email, pageable)
                );


        assertEquals(
                "Trainer profile not found",
                exception.getMessage()
        );
    }


    // GET ALL / GET BY ID //


    @Test
    void shouldGetAllBookingsSuccessfully() {

        Pageable pageable = PageRequest.of(0, 10);

        Booking booking = new Booking();

        booking.setId(100L);

        Page<Booking> bookings =
                new PageImpl<>(
                        List.of(booking),
                        pageable,
                        1
                );


        when(repository.findAll(pageable))
                .thenReturn(bookings);

        when(mapper.toResponse(booking))
                .thenReturn(response(BookingStatus.BOOKED));


        Page<BookingResponse> result =
                bookingService.getAllBookings(pageable);


        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(100L, result.getContent()
                .get(0)
                .getId()
        );
    }


    @Test
    void shouldGetBookingByIdSuccessfully() {

        Booking booking = new Booking();

        booking.setId(100L);

        BookingResponse response = response(BookingStatus.BOOKED);


        when(repository.findById(100L))
                .thenReturn(Optional.of(booking));

        when(mapper.toResponse(booking))
                .thenReturn(response);


        BookingResponse result = bookingService.getBookingById(100L);


        assertEquals(
                100L,
                result.getId()
        );

        verify(repository).findById(100L);

        verify(mapper).toResponse(booking);
    }


    @Test
    void shouldRejectGetBookingByIdWhenNotFound() {

        when(repository.findById(100L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> bookingService.getBookingById(100L)
                );


        assertEquals(
                "Booking not found",
                exception.getMessage()
        );

        verifyNoInteractions(mapper);
    }


    // CANCEL //

    @Test
    void shouldCancelBookingSuccessfully() {

        String email = "student@test.com";

        Student student = student();

        AppUser user = studentUser(email, student);

        Booking booking =
                new Booking();

        booking.setId(100L);
        booking.setStudent(student);
        booking.setTrainingSession(
                scheduledSession()
        );
        booking.setStatus(
                BookingStatus.BOOKED
        );


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(repository
                .findByStudent_IdAndTrainingSession_Id(
                        1L,
                        10L
                ))
                .thenReturn(Optional.of(booking));

        when(repository.save(booking))
                .thenReturn(booking);

        when(mapper.toResponse(booking))
                .thenReturn(response(BookingStatus.CANCELLED));


        BookingResponse result = bookingService.cancelBooking(request(), email);


        assertEquals(
                BookingStatus.CANCELLED,
                booking.getStatus()
        );

        assertEquals(
                BookingStatus.CANCELLED,
                result.getStatus()
        );

        verify(repository)
                .save(booking);
    }


    @Test
    void shouldReturnAlreadyCancelledBookingWithoutSaving() {

        String email = "student@test.com";

        Student student = student();

        AppUser user = studentUser(email, student);

        Booking booking =
                new Booking();

        booking.setId(100L);
        booking.setStudent(student);
        booking.setTrainingSession(
                scheduledSession()
        );
        booking.setStatus(BookingStatus.CANCELLED);


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(repository
                .findByStudent_IdAndTrainingSession_Id(
                        1L,
                        10L
                ))
                .thenReturn(Optional.of(booking));

        when(mapper.toResponse(booking))
                .thenReturn(response(BookingStatus.CANCELLED)
                );


        BookingResponse result =
                bookingService.cancelBooking(
                        request(),
                        email
                );


        assertEquals(
                BookingStatus.CANCELLED,
                result.getStatus()
        );

        verify(repository, never())
                .save(any(Booking.class));
    }


    @Test
    void shouldRejectCancelWhenBookingNotFound() {

        String email = "student@test.com";

        Student student = student();

        AppUser user = studentUser(email, student);


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(repository
                .findByStudent_IdAndTrainingSession_Id(
                        1L,
                        10L
                ))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> bookingService.cancelBooking(request(), email)
                );


        assertEquals(
                "Booking not found",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(Booking.class));
    }


    @Test
    void shouldRejectCancelWhenUserNotFound() {

        String email = "missing@test.com";

        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> bookingService.cancelBooking(request(), email)
                );


        assertEquals("User not found", exception.getMessage());

        verify(repository, never())
                .save(any(Booking.class));
    }


    @Test
    void shouldRejectCancelWhenStudentProfileNotFound() {

        String email = "trainer@test.com";

        AppUser user = new AppUser();

        user.setEmail(email);
        user.setStudent(null);


        when(appUserRepository.findByEmail(email))
                .thenReturn(Optional.of(user));


        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> bookingService.cancelBooking(request(), email)
                );

        assertEquals(
                "Student profile not found",
                exception.getMessage()
        );

        verify(repository, never())
                .save(any(Booking.class));
    }
}