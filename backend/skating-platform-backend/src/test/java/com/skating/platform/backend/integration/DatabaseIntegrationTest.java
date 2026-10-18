
package com.skating.platform.backend.integration;

import com.skating.platform.backend.dto.booking.request.CreateBookingRequest;
import com.skating.platform.backend.dto.booking.response.BookingResponse;
import com.skating.platform.backend.dto.service.response.SchoolServiceResponse;
import com.skating.platform.backend.entity.*;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.repository.*;
import com.skating.platform.backend.service.BookingService;
import com.skating.platform.backend.service.SchoolServiceService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class DatabaseIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("skating_test")
                    .withUsername("test_user")
                    .withPassword("test_password");


    @Autowired
    private DataSource dataSource;

    @Autowired
    private SchoolServiceRepository schoolServiceRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SchoolServiceService schoolServiceService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TrainerRepository trainerRepository;

    @Autowired
    private TrainingSessionRepository trainingSessionRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyLiquibaseMigrations() throws Exception {

        String sql = """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                      'users',
                      'students',
                      'trainers',
                      'school_services',
                      'training_sessions',
                      'bookings',
                      'trainers_services'
                  )
                """;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            assertTrue(result.next());

            assertEquals(7L, result.getLong(1));
        }
    }


    @Test
    @Transactional
    void shouldSaveAndFindSchoolService() {

        SchoolService service = new SchoolService(
                "Individual skating lesson",
                "One-on-one training",
                new BigDecimal("2500.00"),
                "INDIVIDUAL"
        );

        SchoolService saved = schoolServiceRepository.saveAndFlush(service);

        Long savedId = saved.getId();

        // Очищаем контекст хранения JPA
        entityManager.clear();

        Optional<SchoolService> found = schoolServiceRepository.findById(savedId);

        assertNotNull(savedId);
        assertTrue(found.isPresent());

        assertEquals("Individual skating lesson", found.get().getName());
        assertEquals("INDIVIDUAL", found.get().getType());
        assertEquals(new BigDecimal("2500.00"), found.get().getPrice());
        assertTrue(found.get().getActive());
    }


    @Test
    @Transactional
    void shouldFilterSchoolServicesUsingRealDatabase() {

        SchoolService matching = new SchoolService(
                "Figure skating",
                "Individual lesson",
                new BigDecimal("2500.00"),
                "INDIVIDUAL"
        );

        SchoolService wrongType = new SchoolService(
                "Group skating",
                "Group lesson",
                new BigDecimal("2500.00"),
                "GROUP"
        );

        SchoolService wrongPrice = new SchoolService(
                "Premium skating",
                "Expensive lesson",
                new BigDecimal("5500.00"),
                "INDIVIDUAL"
        );

        SchoolService inactive = new SchoolService(
                "Inactive skating",
                "Unavailable lesson",
                new BigDecimal("2600.00"),
                "INDIVIDUAL"
        );
        inactive.setActive(false);

        SchoolService wrongName = new SchoolService(
                "Hockey lesson",
                "Individual hockey lesson",
                new BigDecimal("2500.00"),
                "INDIVIDUAL"
        );

        schoolServiceRepository.saveAllAndFlush(
                List.of(
                        matching,
                        wrongType,
                        wrongPrice,
                        inactive,
                        wrongName
                )
        );

        entityManager.clear();

        Page<SchoolServiceResponse> result =
                schoolServiceService.filterServices(
                        "SKATING",
                        "individual",
                        new BigDecimal("2000.00"),
                        new BigDecimal("3000.00"),
                        true,
                        PageRequest.of(0, 10)
                );

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(
                matching.getId(),
                result.getContent().get(0).getId()
        );
    }

    @Test
    @Transactional
    void shouldCreateBookingInRealDatabase() {

        Student student = new Student();
        student.setFirstName("Mikhail");
        student.setLastName("Ivanov");
        student.setPhone("+79991234567");
        student.setActive(true);

        student = studentRepository.saveAndFlush(student);


        Trainer trainer = new Trainer();
        trainer.setFirstName("Anna");
        trainer.setLastName("Petrova");
        trainer.setPhone("+79997654321");
        trainer.setExperienceYears(8);
        trainer.setActive(true);
        trainer.setServices(new HashSet<>());

        trainer = trainerRepository.saveAndFlush(trainer);

        SchoolService service = new SchoolService(
                "Individual figure skating",
                "Private lesson with a trainer",
                new BigDecimal("2500.00"),
                "INDIVIDUAL"
        );

        service = schoolServiceRepository.saveAndFlush(service);


        TrainingSession session = new TrainingSession();
        session.setTrainer(trainer);
        session.setService(service);
        session.setStartTime(LocalDateTime.of(2026, 10, 25, 15, 0));
        session.setEndTime(LocalDateTime.of(2026, 10, 25, 16, 0));
        session.setStatus(TrainingSessionStatus.SCHEDULED);
        session.setCapacity(10);

        session = trainingSessionRepository.saveAndFlush(session);


        AppUser user = new AppUser();
        user.setEmail("student.integration@test.com");
        user.setPassword("test-hash");
        user.setRole(Role.STUDENT);
        user.setActive(true);
        user.setStudent(student);

        user = appUserRepository.saveAndFlush(user);

        CreateBookingRequest request = new CreateBookingRequest();

        request.setTrainingSessionId(session.getId());

        BookingResponse result = bookingService.createBooking(request, "student.integration@test.com");

        entityManager.flush();
        entityManager.clear();
        Optional<Booking> found = bookingRepository.findByStudent_IdAndTrainingSession_Id(student.getId(), session.getId());

        assertTrue(found.isPresent());

        Booking savedBooking = found.get();

        assertEquals(result.getId(), savedBooking.getId());
        assertEquals(student.getId(), savedBooking.getStudent().getId());
        assertEquals(session.getId(), savedBooking.getTrainingSession().getId());
        assertEquals(BookingStatus.BOOKED, savedBooking.getStatus());
        assertNotNull(savedBooking.getBookedAt());

        assertNotNull(result.getId());
        assertEquals(session.getId(), result.getTrainingSessionId());
        assertEquals(BookingStatus.BOOKED, result.getStatus());

        Page<Booking> studentBookings = bookingRepository.findByStudent_Id(student.getId(), PageRequest.of(0, 10));
        assertEquals(1, studentBookings.getTotalElements());
        assertEquals(result.getId(), studentBookings.getContent().get(0).getId());

        Page<Booking> trainerBookings = bookingRepository.findByTrainingSession_Trainer_Id(trainer.getId(), PageRequest.of(0, 10));
        assertEquals(1, trainerBookings.getTotalElements());
        assertEquals(session.getId(), trainerBookings.getContent().get(0).getTrainingSession().getId());

        assertEquals(student.getId(), result.getStudentId());
        assertEquals(session.getId(), result.getTrainingSessionId());
        assertNotNull(result.getBookedAt());


        ConflictException exception = assertThrows(ConflictException.class,
                () -> bookingService.createBooking(request, "student.integration@test.com"));

        assertEquals(
                "Student is already booked for this training session",
                exception.getMessage()
        );

        long bookedCount = bookingRepository.countByTrainingSession_IdAndStatus(session.getId(), BookingStatus.BOOKED);

        assertEquals(1L, bookedCount);

    }


    private Student createStudentAccount(String email, String phone) {
        Student student = new Student();
        student.setFirstName("Test");
        student.setLastName("Student");
        student.setPhone(phone);
        student.setActive(true);

        student = studentRepository.saveAndFlush(student);

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword("test-hash");
        user.setRole(Role.STUDENT);
        user.setActive(true);
        user.setStudent(student);

        appUserRepository.saveAndFlush(user);

        return student;
    }

    private TrainingSession createScheduledSession(int capacity) {
        Trainer trainer = new Trainer();
        trainer.setFirstName("Anna");
        trainer.setLastName("Petrova");
        trainer.setPhone("+79997654321");
        trainer.setExperienceYears(8);
        trainer.setActive(true);
        trainer.setServices(new HashSet<>());

        trainer = trainerRepository.saveAndFlush(trainer);

        SchoolService service = new SchoolService(
                "Individual figure skating",
                "Private lesson",
                new BigDecimal("2500.00"),
                "INDIVIDUAL"
        );

        service = schoolServiceRepository.saveAndFlush(service);

        TrainingSession session = new TrainingSession();
        session.setTrainer(trainer);
        session.setService(service);
        session.setStartTime(LocalDateTime.of(2026, 10, 25, 15, 0));
        session.setEndTime(LocalDateTime.of(2026, 10, 25, 16, 0));
        session.setStatus(TrainingSessionStatus.SCHEDULED);
        session.setCapacity(capacity);

        return trainingSessionRepository.saveAndFlush(session);
    }

    @Test
    @Transactional
    void shouldRejectBookingWhenSessionIsFull() {

        String firstEmail = "first@test.com";
        String secondEmail = "second@test.com";

        createStudentAccount(firstEmail, "+799932543444");
        Student secondStudent = createStudentAccount(secondEmail, "+79992324534");

        TrainingSession session = createScheduledSession(1);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setTrainingSessionId(session.getId());

        bookingService.createBooking(request, firstEmail);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> bookingService.createBooking(request, secondEmail)
        );

        assertEquals("Training session is full", exception.getMessage());

        assertEquals(1L, bookingRepository.countByTrainingSession_IdAndStatus(session.getId(), BookingStatus.BOOKED));

        assertTrue(bookingRepository.findByStudent_IdAndTrainingSession_Id(secondStudent.getId(), session.getId()).isEmpty());
    }


    @Test
    @Transactional
    void shouldCancelAndRebookExistingBooking() {

        String email = "student@test.com";

        Student student =
                createStudentAccount(email, "+79991111111");

        TrainingSession session = createScheduledSession(1);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setTrainingSessionId(session.getId());

        BookingResponse original =
                bookingService.createBooking(request, email);

        BookingResponse cancelled =
                bookingService.cancelBooking(request, email);

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertEquals(original.getId(), cancelled.getId());

        assertEquals(0L, bookingRepository.countByTrainingSession_IdAndStatus(session.getId(), BookingStatus.BOOKED));

        BookingResponse rebooked = bookingService.createBooking(request, email);

        assertEquals(BookingStatus.BOOKED, rebooked.getStatus());

        assertEquals(original.getId(), rebooked.getId());

        entityManager.flush();
        entityManager.clear();

        Optional<Booking> saved = bookingRepository.findByStudent_IdAndTrainingSession_Id(student.getId(), session.getId());

        assertTrue(saved.isPresent());
        assertEquals(original.getId(), saved.get().getId());
        assertEquals(BookingStatus.BOOKED, saved.get().getStatus());
    }



    @Test
    @Transactional
    void shouldRejectDuplicateUserEmailInDatabase() {

        jdbcTemplate.update(
                """
                INSERT INTO users (email, password, role, active)
                VALUES (?, ?, ?, ?)
                """,
                "duplicate@test.com",
                "test-hash",
                "STUDENT",
                true
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO users (email, password, role, active)
                        VALUES (?, ?, ?, ?)
                        """,
                        "duplicate@test.com",
                        "another-hash",
                        "STUDENT",
                        true
                )
        );
    }


    @Test
    @Transactional
    void shouldRejectDuplicateStudentPhoneInDatabase() {

        jdbcTemplate.update(
                """
                INSERT INTO students
                    (first_name, last_name, phone, email, active)
                VALUES (?, ?, ?, ?, ?)
                """,
                "Mikhail",
                "Ivanov",
                "+79990001111",
                "first@test.com",
                true
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO students
                            (first_name, last_name, phone, email, active)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                        "Petr",
                        "Sidorov",
                        "+79990001111",
                        "second@test.com",
                        true
                )
        );
    }


    @Test
    @Transactional
    void shouldRejectDuplicateBookingInDatabase() {

        String email = "booking.constraint@test.com";

        Student student = createStudentAccount(
                email,
                "+79990002222"
        );

        TrainingSession session = createScheduledSession(10);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setTrainingSessionId(session.getId());

        bookingService.createBooking(request, email);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO bookings
                            (student_id, training_session_id, booked_at, status)
                        VALUES (?, ?, ?, ?)
                        """,
                        student.getId(),
                        session.getId(),
                        LocalDateTime.now(),
                        "BOOKED"
                )
        );
    }

}
