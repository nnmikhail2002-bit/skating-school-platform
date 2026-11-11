
package com.skating.platform.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class PaymentFlowIntegrationTest {

    private static final URI BACKEND_URL = URI.create("http://localhost:8080/");

    private static final BigDecimal AMOUNT = new BigDecimal("1500.00");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

    private static Map<String, String> env;
    private static String expectedPaymentStatus;

    private enum Database {
        MAIN,
        PAYMENTS
    }

    private static class TestData {
        Long serviceId;
        Long trainerId;
        Long sessionId;
        Long bookingId;
        String email;
        String phone;
        String paymentAttemptId;
    }

    @BeforeAll
    static void loadEnvironment() throws IOException {

        Path root = findProjectRoot();

        Path envFile = root.resolve(".env");

        if (!Files.exists(envFile)) {
            throw new IllegalStateException(".env not found");
        }

        env = new HashMap<>();

        for (String rawLine :
                Files.readAllLines(envFile)) {

            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("export ")) {
                line = line.substring(7).trim();
            }

            int separator = line.indexOf('=');

            if (separator <= 0) {
                continue;
            }

            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();

            if (value.length() >= 2) {

                char first = value.charAt(0);

                if ((first == '"' || first == '\'')
                        && value.charAt(value.length() - 1) == first) {

                    value = value.substring(1, value.length() - 1);
                }
            }

            env.put(key, value);
        }

        expectedPaymentStatus = env.getOrDefault(
                "PAYMENT_MOCK_FINAL_STATUS",
                "SUCCEEDED"
        ).trim().toUpperCase(Locale.ROOT);

        if (!expectedPaymentStatus.equals("SUCCEEDED")
                && !expectedPaymentStatus.equals("FAILED")) {
            throw new IllegalStateException(
                    "Unsupported payment status: " + expectedPaymentStatus
            );
        }
    }

    @Test
    void shouldCompletePaymentAcrossBothServices() throws Exception {

        TestData data = new TestData();

        data.email = "integration-" + UUID.randomUUID() + "@example.com";

        data.phone = generatePhone();

        boolean completed = false;

        try {

            createTrainingFixture(data);

            HttpResponse<String> registration =
                    postJson(
                            "/api/auth/register",
                            """
                            {
                                "firstName": "Integration",
                                "lastName": "Student",
                                "phone": "%s",
                                "email": "%s",
                                "password": "IntegrationTest123!"
                            }
                            """.formatted(data.phone, data.email),
                            null
                    );

            assertHttpStatus(201, registration);

            HttpResponse<String> login =
                    postJson(
                            "/api/auth/login",
                            """
                            {
                                "email": "%s",
                                "password": "IntegrationTest123!"
                            }
                            """.formatted(data.email),
                            null
                    );

            assertHttpStatus(200, login);

            String token = extractString(login.body(), "token");

            HttpResponse<String> booking =
                    postJson(
                            "/api/bookings",
                            """
                            {
                                "trainingSessionId": %d
                            }
                            """.formatted(data.sessionId),
                            token
                    );

            assertHttpStatus(201, booking);

            data.bookingId = extractLong(booking.body(), "id");

            assertEquals("PENDING_PAYMENT", extractString(booking.body(), "status"));

            data.paymentAttemptId = queryString(
                    Database.MAIN,
                    """
                    SELECT payment_attempt_id::text
                    FROM bookings
                    WHERE id = ?
                    """,
                    data.bookingId
            );

            assertNotNull(data.paymentAttemptId, "Payment attempt ID was not created");

            String expectedBookingStatus = expectedPaymentStatus.equals("SUCCEEDED") ? "BOOKED" : "PAYMENT_FAILED";

            String expectedOutboxEventType = expectedPaymentStatus.equals("SUCCEEDED") ? "PAYMENT_COMPLETED" : "PAYMENT_FAILED";

            assertEquals(
                    1L,
                    queryLong(
                            Database.MAIN,
                            """
                            SELECT COUNT(*)
                            FROM outbox_events
                            WHERE aggregate_id = ?
                              AND event_type = 'PAYMENT_REQUESTED'
                            """,
                            String.valueOf(data.bookingId)
                    )
            );

            await().alias("Payment flow across Main Backend and Payment Service")
                    .atMost(Duration.ofSeconds(180))
                    .pollInterval(Duration.ofSeconds(1))
                    .untilAsserted(() -> {

                        // Main Backend
                        assertEquals(
                                expectedBookingStatus,
                                queryString(
                                        Database.MAIN,
                                        """
                                        SELECT status
                                        FROM bookings
                                        WHERE id = ?
                                        """,
                                        data.bookingId
                                )
                        );

                        // Payment Service
                        assertEquals(
                                expectedPaymentStatus,
                                queryString(
                                        Database.PAYMENTS,
                                        """
                                        SELECT status
                                        FROM payment_transactions
                                        WHERE idempotency_key = ?
                                        """,
                                        data.paymentAttemptId
                                )
                        );

                        // Проверяем отсутствие дублирующих платежей
                        assertEquals(
                                1L,
                                queryLong(
                                        Database.PAYMENTS,
                                        """
                                        SELECT COUNT(*)
                                        FROM payment_transactions
                                        WHERE idempotency_key = ?
                                        """,
                                        data.paymentAttemptId
                                )
                        );

                        // Проверяем публикацию события Backend
                        assertEquals(
                                1L,
                                queryLong(
                                        Database.MAIN,
                                        """
                                        SELECT COUNT(*)
                                        FROM outbox_events
                                        WHERE aggregate_id = ?
                                          AND event_type = 'PAYMENT_REQUESTED'
                                          AND published_at IS NOT NULL
                                        """,
                                        String.valueOf(data.bookingId)
                                )
                        );

                        // Проверяем публикацию результата оплаты
                        assertEquals(
                                1L,
                                queryLong(
                                        Database.PAYMENTS,
                                        """
                                        SELECT COUNT(*)
                                        FROM outbox_events
                                        WHERE event_type = ?
                                          AND aggregate_id = (
                                              SELECT id::text
                                              FROM payment_transactions
                                              WHERE idempotency_key = ?
                                          )
                                          AND published_at IS NOT NULL
                                        """,
                                        expectedOutboxEventType,
                                        data.paymentAttemptId
                                )
                        );
                    });

            completed = true;

        } finally {

            if (completed) {
                cleanup(data);
            } else {
                System.err.println(
                        "Integration test failed. Test data retained. "
                                + "Booking ID: " + data.bookingId
                                + ", email: " + data.email
                );
            }
        }
    }

    private static void createTrainingFixture(TestData data) throws SQLException {

        String suffix = UUID.randomUUID().toString();

        data.serviceId = insertReturningId(
                Database.MAIN,
                """
                INSERT INTO school_services
                    (name, description, price, type, active)
                VALUES (?, ?, ?, ?, TRUE)
                RETURNING id
                """,
                "Integration service " + suffix,
                "Temporary integration test service",
                AMOUNT,
                "GROUP"
        );

        data.trainerId = insertReturningId(
                Database.MAIN,
                """
                INSERT INTO trainers
                    (first_name, last_name, phone,
                     experience_years, active)
                VALUES (?, ?, ?, ?, TRUE)
                RETURNING id
                """,
                "Integration",
                "Trainer",
                generatePhone(),
                5
        );

        execute(
                Database.MAIN,
                """
                INSERT INTO trainers_services
                    (trainer_id, service_id)
                VALUES (?, ?)
                """,
                data.trainerId,
                data.serviceId
        );

        LocalDateTime startTime = LocalDateTime.now()
                        .plusDays(2)
                        .withNano(0);

        LocalDateTime endTime = startTime.plusHours(1);

        data.sessionId = insertReturningId(
                Database.MAIN,
                """
                INSERT INTO training_sessions
                    (trainer_id, service_id, start_time,
                     end_time, status, capacity)
                VALUES (?, ?, ?, ?, 'SCHEDULED', 5)
                RETURNING id
                """,
                data.trainerId,
                data.serviceId,
                Timestamp.valueOf(startTime),
                Timestamp.valueOf(endTime)
        );
    }


    private static HttpResponse<String> postJson(
            String path,
            String json,
            String token
    ) throws IOException, InterruptedException {

        HttpRequest.Builder builder = HttpRequest.newBuilder(BACKEND_URL.resolve(path))
                        .timeout(Duration.ofSeconds(20))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json));

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        return HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void assertHttpStatus(int expected, HttpResponse<String> response) {

        assertEquals(
                expected,
                response.statusCode(),
                () -> "Unexpected HTTP response: "
                        + response.statusCode()
                        + "\nBody: "
                        + response.body()
        );
    }

    private static String extractString(String json, String field) {

        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(field)
                        + "\"\\s*:\\s*\"([^\"]+)\""
        );

        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            throw new IllegalStateException("JSON field not found: " + field + "\nResponse: " + json);
        }

        return matcher.group(1);
    }

    private static long extractLong(String json, String field) {

        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(field)
                        + "\"\\s*:\\s*(\\d+)"
        );

        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            throw new IllegalStateException("JSON field not found: " + field + "\nResponse: " + json);
        }

        return Long.parseLong(matcher.group(1));
    }


    private static Connection connection(Database database) throws SQLException {

        String prefix;
        int port;

        if (database == Database.MAIN) {
            prefix = "POSTGRES_";
            port = 5432;
        } else {
            prefix = "PAYMENTS_POSTGRES_";
            port = 5433;
        }

        String url = "jdbc:postgresql://localhost:" + port + "/" + required(prefix + "DB");

        return DriverManager.getConnection(url, required(prefix + "USER"), required(prefix + "PASSWORD"));
    }

    private static String required(String key) {

        String value = System.getenv(key);

        if (value == null || value.isBlank()) {
            value = env.get(key);
        }

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing configuration: " + key);
        }

        return value;
    }

    private static Path findProjectRoot() {

        Path current = Path.of("")
                .toAbsolutePath()
                .normalize();

        while (current != null) {

            if (Files.isRegularFile(
                    current.resolve("compose.yaml")
            )) {
                return current;
            }

            current = current.getParent();
        }

        throw new IllegalStateException("Project root with compose.yaml not found");
    }

    private static long insertReturningId(
            Database database,
            String sql,
            Object... parameters
    ) throws SQLException {

        try (Connection connection = connection(database);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new SQLException("Insert did not return ID");
                }

                return resultSet.getLong(1);
            }
        }
    }

    private static void execute(Database database, String sql, Object... parameters) throws SQLException {

        try (Connection connection = connection(database);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setParameters(statement, parameters);
            statement.executeUpdate();
        }
    }

    private static String queryString(Database database, String sql, Object... parameters) throws SQLException {

        try (Connection connection = connection(database);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {

                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    private static long queryLong(Database database, String sql, Object... parameters) throws SQLException {

        try (Connection connection = connection(database);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new SQLException("Query returned no rows");
                }

                return resultSet.getLong(1);
            }
        }
    }

    private static void setParameters(PreparedStatement statement, Object... parameters) throws SQLException {

        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }

    private static void cleanup(TestData data)
            throws SQLException {

        if (data.paymentAttemptId != null) {

            String paymentId = queryString(
                    Database.PAYMENTS,
                    """
                    SELECT id::text
                    FROM payment_transactions
                    WHERE idempotency_key = ?
                    """,
                    data.paymentAttemptId
            );

            if (paymentId != null) {

                execute(
                        Database.PAYMENTS,
                        """
                        DELETE FROM outbox_events
                        WHERE aggregate_id = ?
                        """,
                        paymentId
                );
            }

            execute(
                    Database.PAYMENTS,
                    """
                    DELETE FROM payment_transactions
                    WHERE idempotency_key = ?
                    """,
                    data.paymentAttemptId
            );
        }

        if (data.bookingId != null) {

            execute(
                    Database.MAIN,
                    """
                    DELETE FROM outbox_events
                    WHERE aggregate_id = ?
                    """,
                    String.valueOf(data.bookingId)
            );

            execute(
                    Database.MAIN,
                    "DELETE FROM bookings WHERE id = ?",
                    data.bookingId
            );
        }

        if (data.email != null) {

            execute(
                    Database.MAIN,
                    "DELETE FROM users WHERE email = ?",
                    data.email
            );

            execute(
                    Database.MAIN,
                    "DELETE FROM students WHERE email = ?",
                    data.email
            );
        }

        if (data.sessionId != null) {
            execute(
                    Database.MAIN,
                    "DELETE FROM training_sessions WHERE id = ?",
                    data.sessionId
            );
        }

        if (data.trainerId != null && data.serviceId != null) {

            execute(
                    Database.MAIN,
                    """
                    DELETE FROM trainers_services
                    WHERE trainer_id = ? AND service_id = ?
                    """,
                    data.trainerId,
                    data.serviceId
            );
        }

        if (data.trainerId != null) {
            execute(
                    Database.MAIN,
                    "DELETE FROM trainers WHERE id = ?",
                    data.trainerId
            );
        }

        if (data.serviceId != null) {
            execute(
                    Database.MAIN,
                    "DELETE FROM school_services WHERE id = ?",
                    data.serviceId
            );
        }
    }

    private static String generatePhone() {

        long number = ThreadLocalRandom.current().nextLong(1_000_000_000L);

        return "+79" + String.format(Locale.ROOT, "%09d", number);
    }
}
