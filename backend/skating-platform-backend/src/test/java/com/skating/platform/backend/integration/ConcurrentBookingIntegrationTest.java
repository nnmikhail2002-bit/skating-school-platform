
package com.skating.platform.backend.integration;

import com.skating.platform.backend.dto.booking.request.CreateBookingRequest;
import com.skating.platform.backend.dto.booking.response.BookingResponse;
import com.skating.platform.backend.entity.*;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.repository.*;
import com.skating.platform.backend.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
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
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class ConcurrentBookingIntegrationTest {

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
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TrainerRepository trainerRepository;

    @Autowired
    private SchoolServiceRepository schoolServiceRepository;

    @Autowired
    private TrainingSessionRepository trainingSessionRepository;

    @Autowired
    private AppUserRepository appUserRepository;


    @Test
    void shouldPreventOverbookingUnderConcurrentRequests() throws Exception {

        String firstEmail = "first.concurrent@test.com";
        String secondEmail = "second.concurrent@test.com";

        createStudentAccount(firstEmail, "+79991111111");
        createStudentAccount(secondEmail, "+79992222222");

        TrainingSession session = createSessionWithOnePlace();

        CreateBookingRequest request = new CreateBookingRequest();
        request.setTrainingSessionId(session.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            try (Connection blocker = dataSource.getConnection()) {

                blocker.setAutoCommit(false);

                try (Statement statement = blocker.createStatement()) {
                    statement.execute(
                            "LOCK TABLE public.bookings IN SHARE MODE"
                    );
                }

                Future<BookingResponse> first = executor.submit(() -> {
                    start.await();
                    return bookingService.createBooking(request, firstEmail);
                });

                Future<BookingResponse> second = executor.submit(() -> {
                    start.await();
                    return bookingService.createBooking(request, secondEmail);
                });

                start.countDown();

                try {
                    awaitTwoBlockedInserts(blocker);
                } finally {
                    blocker.rollback();
                }

                int successfulBookings = 0;
                int conflicts = 0;

                for (Future<BookingResponse> future : List.of(first, second)) {
                    try {
                        future.get(10, TimeUnit.SECONDS);
                        successfulBookings++;
                    } catch (ExecutionException exception) {
                        if (exception.getCause() instanceof ConflictException) {
                            conflicts++;
                        } else {
                            throw exception;
                        }
                    }
                }

                long bookedCount = bookingRepository
                        .countByTrainingSession_IdAndStatus(
                                session.getId(),
                                BookingStatus.BOOKED
                        );

                assertEquals(1, successfulBookings);
                assertEquals(1, conflicts);
                assertEquals(1L, bookedCount);
            }
        } finally {
            executor.shutdownNow();
        }
    }


    private void awaitTwoBlockedInserts(Connection connection)
            throws Exception {

        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(15);

        String sql = """
                SELECT COUNT(*)
                FROM pg_locks
                WHERE relation = 'public.bookings'::regclass
                  AND mode = 'RowExclusiveLock'
                  AND granted = false
                """;

        while (System.nanoTime() < deadline) {

            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(sql)) {

                result.next();

                if (result.getLong(1) >= 2L) {
                    return;
                }
            }

            Thread.sleep(50);
        }

        fail("Both booking INSERT operations did not reach the lock");
    }


    private void createStudentAccount(String email, String phone) {

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
    }


    private TrainingSession createSessionWithOnePlace() {

        Trainer trainer = new Trainer();
        trainer.setFirstName("Anna");
        trainer.setLastName("Petrova");
        trainer.setPhone("+79993333333");
        trainer.setExperienceYears(8);
        trainer.setActive(true);
        trainer.setServices(new HashSet<>());

        trainer = trainerRepository.saveAndFlush(trainer);

        SchoolService service = new SchoolService(
                "Individual skating",
                "Concurrent booking test",
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
        session.setCapacity(1);

        return trainingSessionRepository.saveAndFlush(session);
    }
}
