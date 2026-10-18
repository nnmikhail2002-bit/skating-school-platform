
package com.skating.platform.backend.integration;

import com.jayway.jsonpath.JsonPath;
import com.skating.platform.backend.entity.*;
import com.skating.platform.backend.repository.AppUserRepository;
import com.skating.platform.backend.repository.SchoolServiceRepository;
import com.skating.platform.backend.repository.TrainerRepository;
import com.skating.platform.backend.repository.TrainingSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("skating_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private TrainerRepository trainerRepository;

    @Autowired
    private SchoolServiceRepository schoolServiceRepository;

    @Autowired
    private TrainingSessionRepository trainingSessionRepository;

    @Test
    @Transactional
    void shouldRegisterLoginAndAccessStudentProfile() throws Exception {

        String email = "auth.integration@test.com";
        String password = "strongPassword123";

        // 1. Регистрация
        String registrationJson = """
                {
                    "firstName": "Mikhail",
                    "lastName": "Ivanov",
                    "phone": "+79991234567",
                    "email": "auth.integration@test.com",
                    "password": "strongPassword123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(registrationJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow();

        assertEquals(Role.STUDENT, user.getRole());
        assertNotNull(user.getStudent());

        assertNotEquals(password, user.getPassword());

        assertTrue(passwordEncoder.matches(password, user.getPassword()));

        String loginJson = """
                {
                    "email": "auth.integration@test.com",
                    "password": "strongPassword123"
                }
                """;

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(loginJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");

        Jwt decoded = jwtDecoder.decode(token);

        assertEquals(email, decoded.getSubject());
        assertEquals("STUDENT", decoded.getClaimAsString("role"));

        mockMvc.perform(get("/api/students/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("Mikhail"))
                .andExpect(jsonPath("$.lastName").value("Ivanov"));
    }

    private void registerTestStudent() throws Exception {

        String json = """
            {
                "firstName": "Mikhail",
                "lastName": "Ivanov",
                "phone": "+79991234567",
                "email": "auth.integration@test.com",
                "password": "strongPassword123"
            }
            """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated());
    }


    private String loginTestStudent() throws Exception {

        String json = """
            {
                "email": "auth.integration@test.com",
                "password": "strongPassword123"
            }
            """;

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.token"
        );
    }


    @Test
    @Transactional
    void shouldRejectLoginWithWrongPassword() throws Exception {

        registerTestStudent();

        String json = """
            {
                "email": "auth.integration@test.com",
                "password": "wrongPassword123"
            }
            """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password"));
    }


    @Test
    void shouldRejectAccessWithoutToken() throws Exception {

        mockMvc.perform(
                        get("/api/students/me")
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldRejectAccessWithInvalidToken() throws Exception {

        mockMvc.perform(
                        get("/api/students/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer invalid.jwt.token"
                                )
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    @Transactional
    void shouldForbidStudentFromReadingAllStudents() throws Exception {

        registerTestStudent();

        String token = loginTestStudent();

        mockMvc.perform(
                        get("/api/students")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isForbidden());
    }



    private TrainingSession createScheduledSession() {

        Trainer trainer = new Trainer();
        trainer.setFirstName("Anna");
        trainer.setLastName("Petrova");
        trainer.setPhone("+79997776655");
        trainer.setExperienceYears(8);
        trainer.setActive(true);
        trainer.setServices(new HashSet<>());

        trainer = trainerRepository.saveAndFlush(trainer);

        SchoolService service = new SchoolService(
                "Individual figure skating",
                "Training with a personal coach",
                new BigDecimal("2500.00"),
                "INDIVIDUAL"
        );

        service = schoolServiceRepository.saveAndFlush(service);

        TrainingSession session = new TrainingSession();
        session.setTrainer(trainer);
        session.setService(service);
        session.setStartTime(LocalDateTime.of(2026, 10, 25, 15, 0));
        session.setEndTime(LocalDateTime.of(2026, 10, 25, 16, 0));
        session.setCapacity(10);
        session.setStatus(TrainingSessionStatus.SCHEDULED);

        return trainingSessionRepository.saveAndFlush(session);
    }



    @Test
    @Transactional
    void shouldCreateReadAndCancelBookingThroughHttp() throws Exception {

        registerTestStudent();

        String token = loginTestStudent();
        String email = "auth.integration@test.com";

        Long studentId = appUserRepository.findByEmail(email)
                .orElseThrow()
                .getStudent()
                .getId();


        TrainingSession session = createScheduledSession();


        String bookingJson = """
            {
                "trainingSessionId": %d
            }
            """.formatted(session.getId());


        MvcResult bookingResult = mockMvc.perform(
                        post("/api/bookings")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(APPLICATION_JSON)
                                .content(bookingJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.studentId")
                        .value(studentId.intValue()))
                .andExpect(jsonPath("$.trainingSessionId")
                        .value(session.getId().intValue()))
                .andReturn();


        Number id = JsonPath.read(bookingResult.getResponse().getContentAsString(), "$.id");

        Long bookingId = id.longValue();


        mockMvc.perform(
                        get("/api/bookings/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(bookingId.intValue()))
                .andExpect(jsonPath("$.content[0].status")
                        .value("BOOKED"));


        mockMvc.perform(
                        patch("/api/bookings/cancel")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(APPLICATION_JSON)
                                .content(bookingJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId.intValue()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(
                        get("/api/bookings/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status")
                        .value("CANCELLED"));
    }


    @Test
    @Transactional
    void shouldNotExposeOtherStudentsBookings() throws Exception {

        // Первый ученик
        registerTestStudent();

        String firstToken = loginTestStudent();

        String firstEmail = "auth.integration@test.com";

        Long firstStudentId = appUserRepository
                .findByEmail(firstEmail)
                .orElseThrow()
                .getStudent()
                .getId();


        String secondRegistrationJson = """
            {
                "firstName": "Petr",
                "lastName": "Sidorov",
                "phone": "+79991234568",
                "email": "second.auth.integration@test.com",
                "password": "strongPassword456"
            }
            """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(secondRegistrationJson)
                )
                .andExpect(status().isCreated());


        String secondLoginJson = """
            {
                "email": "second.auth.integration@test.com",
                "password": "strongPassword456"
            }
            """;

        MvcResult secondLoginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(secondLoginJson)
                )
                .andExpect(status().isOk())
                .andReturn();

        String secondToken = JsonPath.read(secondLoginResult.getResponse().getContentAsString(), "$.token");

        TrainingSession session = createScheduledSession();

        String bookingJson = """
            {
                "trainingSessionId": %d
            }
            """.formatted(session.getId());


        mockMvc.perform(
                        post("/api/bookings")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + firstToken
                                )
                                .contentType(APPLICATION_JSON)
                                .content(bookingJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId")
                        .value(firstStudentId.intValue()))
                .andExpect(jsonPath("$.status").value("BOOKED"));

        mockMvc.perform(
                        get("/api/bookings/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + firstToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].studentId")
                        .value(firstStudentId.intValue()));

        mockMvc.perform(
                        get("/api/bookings/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + secondToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }


}
