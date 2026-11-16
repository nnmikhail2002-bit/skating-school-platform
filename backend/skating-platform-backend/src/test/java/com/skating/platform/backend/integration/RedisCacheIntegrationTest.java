
package com.skating.platform.backend.integration;

import com.skating.platform.backend.entity.*;
import com.skating.platform.backend.dto.service.request.UpdateSchoolServiceRequest;
import com.skating.platform.backend.dto.trainingsession.response.TrainingSessionResponse;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.mapper.SchoolServiceMapper;
import com.skating.platform.backend.mapper.TrainingSessionMapper;
import com.skating.platform.backend.repository.*;
import com.skating.platform.backend.service.SchoolServiceService;
import com.skating.platform.backend.service.TrainingSessionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest(properties = {"spring.kafka.listener.auto-startup=false", "booking.outbox.enabled=false"})
@ActiveProfiles("test")
class RedisCacheIntegrationTest {

    private static final String REDIS_PASSWORD = "redis_integration_test_password";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("skating_redis_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .withCommand(
                            "redis-server",
                            "--requirepass",
                            REDIS_PASSWORD
                    );

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.data.redis.host", redis::getHost);

        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);
    }

    @Autowired
    private SchoolServiceService schoolServiceService;

    @Autowired
    private TrainingSessionService trainingSessionService;

    @Autowired
    private SchoolServiceRepository schoolServiceRepository;

    @Autowired
    private TrainerRepository trainerRepository;

    @Autowired
    private TrainingSessionRepository trainingSessionRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoSpyBean
    private SchoolServiceMapper schoolServiceMapper;

    @MockitoSpyBean
    private TrainingSessionMapper trainingSessionMapper;


    @BeforeEach
    void clearCaches() {

        cacheManager.getCache("schoolServices").clear();

        cacheManager.getCache("trainingSchedule").clear();

        clearInvocations(schoolServiceMapper, trainingSessionMapper);
    }

    private SchoolService createSchoolService() {

        SchoolService service = new SchoolService(
                "Redis test " + UUID.randomUUID(),
                "Redis integration test service",
                new BigDecimal("1500.00"),
                "GROUP"
        );

        return schoolServiceRepository.saveAndFlush(service);
    }


    private TrainingSession createTrainingSession(SchoolService service) {

        Trainer trainer = new Trainer();

        trainer.setFirstName("Redis");
        trainer.setLastName("Test");

        trainer.setPhone("+7" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));

        trainer.setExperienceYears(5);
        trainer.setActive(true);
        trainer.setServices(new HashSet<>());

        trainer = trainerRepository.saveAndFlush(trainer);

        LocalDateTime start = LocalDateTime.now()
                        .plusDays(2)
                        .withSecond(0)
                        .withNano(0);

        TrainingSession session = new TrainingSession();

        session.setTrainer(trainer);
        session.setService(service);
        session.setStartTime(start);
        session.setEndTime(start.plusHours(1));
        session.setCapacity(10);
        session.setStatus(TrainingSessionStatus.SCHEDULED);

        return trainingSessionRepository.saveAndFlush(session);
    }


    @Test
    void shouldCacheSchoolServiceAndEvictBothCachesAfterUpdate() {

        SchoolService service = createSchoolService();

        TrainingSession session = createTrainingSession(service);

        Cache serviceCache = cacheManager.getCache("schoolServices");
        Cache scheduleCache = cacheManager.getCache("trainingSchedule");

        // Первый запрос: Cache Miss
        var firstService = schoolServiceService.getServiceById(service.getId());

        // Второй запрос: Cache Hit
        var secondService = schoolServiceService.getServiceById(service.getId());

        assertNotNull(firstService);
        assertNotNull(secondService);

        verify(schoolServiceMapper, times(1))
                .toResponse(any(SchoolService.class));

        assertNotNull(serviceCache.get("id:" + service.getId()));

        trainingSessionService.getTrainingSessionById(session.getId());

        assertNotNull(scheduleCache.get("id:" + session.getId()));

        String updatedName = "Updated Redis service " + UUID.randomUUID();

        UpdateSchoolServiceRequest request = new UpdateSchoolServiceRequest();

        request.setName(updatedName);
        request.setType("INDIVIDUAL");
        request.setPrice(new BigDecimal("2500.00"));
        request.setDescription("Updated description");

        schoolServiceService.updateService(service.getId(), request);

        System.out.println(
                "schoolServices keys: " +
                        redisTemplate.keys("skating-platform::schoolServices::*")
        );

        System.out.println(
                "trainingSchedule keys: " +
                        redisTemplate.keys("skating-platform::trainingSchedule::*")
        );

        assertNull(serviceCache.get("id:" + service.getId()));

        assertNull(scheduleCache.get("id:" + session.getId()));

        SchoolService updated = schoolServiceRepository.findById(service.getId())
                .orElseThrow();

        assertEquals(updatedName, updated.getName());
    }


    @Test
    void shouldCacheSchoolServicePagesSeparatelyAndSerializeResults() {

        createSchoolService();
        createSchoolService();

        clearInvocations(schoolServiceMapper);

        // Первый запрос: Cache Miss
        Page<?> firstPage =
                schoolServiceService.getAllServices(PageRequest.of(0, 1));

        assertEquals(1, firstPage.getNumberOfElements());

        // Второй запрос: Cache Hit
        Page<?> repeatedPage =
                schoolServiceService.getAllServices(PageRequest.of(0, 1));

        assertEquals(1, repeatedPage.getNumberOfElements());

        // Mapper должен вызваться только один раз
        verify(schoolServiceMapper, times(1))
                .toResponse(any(SchoolService.class));

        // Другая пагинация: новый Cache Miss
        Page<?> largerPage =
                schoolServiceService.getAllServices(PageRequest.of(0, 2));

        assertEquals(2, largerPage.getNumberOfElements());

        // Один вызов для первой страницы + два для второй
        verify(schoolServiceMapper, times(3))
                .toResponse(any(SchoolService.class));
    }


    @Test
    void shouldCacheTrainingSchedulePagesAndSerializeResults() {

        SchoolService service1 = createSchoolService();
        SchoolService service2 = createSchoolService();

        createTrainingSession(service1);
        createTrainingSession(service2);

        clearInvocations(trainingSessionMapper);

        // Первый запрос: Cache Miss
        Page<TrainingSessionResponse> firstSchedule =
                trainingSessionService.getAllTrainingSessions(
                        PageRequest.of(0, 1)
                );

        assertEquals(1, firstSchedule.getNumberOfElements());

        // Второй запрос: Cache Hit
        Page<TrainingSessionResponse> repeatedSchedule =
                trainingSessionService.getAllTrainingSessions(
                        PageRequest.of(0, 1)
                );

        assertEquals(1, repeatedSchedule.getNumberOfElements());

        // Mapper должен вызваться только один раз
        verify(trainingSessionMapper, times(1))
                .toResponse(any(TrainingSession.class));
    }


    @Test
    void shouldCacheTrainingSessionAndRestoreJavaTypes() {

        SchoolService service = createSchoolService();

        TrainingSession session = createTrainingSession(service);

        clearInvocations(trainingSessionMapper);

        TrainingSessionResponse first = trainingSessionService.getTrainingSessionById(session.getId());

        TrainingSessionResponse second = trainingSessionService.getTrainingSessionById(session.getId());

        assertNotNull(first);
        assertNotNull(second);

        assertEquals(
                session.getStartTime(),
                second.getStartTime()
        );

        assertEquals(
                session.getEndTime(),
                second.getEndTime()
        );

        assertEquals(
                TrainingSessionStatus.SCHEDULED,
                second.getStatus()
        );

        assertEquals(
                session.getCapacity(),
                second.getCapacity()
        );

        verify(trainingSessionMapper, times(1))
                .toResponse(any(TrainingSession.class));
    }


    @Test
    void shouldEvictTrainingScheduleAfterDeletingSession() {

        SchoolService service = createSchoolService();

        TrainingSession session = createTrainingSession(service);

        Cache scheduleCache = cacheManager.getCache("trainingSchedule");

        trainingSessionService.getTrainingSessionById(session.getId());

        assertNotNull(scheduleCache.get("id:" + session.getId()));

        trainingSessionService.deleteTrainingSession(session.getId());

        assertNull(scheduleCache.get("id:" + session.getId()));

        assertTrue(trainingSessionRepository.findById(session.getId()).isEmpty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> trainingSessionService.getTrainingSessionById(
                        session.getId()
                )
        );
    }


    @Test
    void shouldApplyCorrectTtlToBothCaches() {

        SchoolService service = createSchoolService();

        TrainingSession session = createTrainingSession(service);

        schoolServiceService.getServiceById(service.getId());

        trainingSessionService.getTrainingSessionById(session.getId());

        String serviceKey = "skating-platform::schoolServices::id:" + service.getId();

        String scheduleKey = "skating-platform::trainingSchedule::id:" + session.getId();

        Long serviceTtl = redisTemplate.getExpire(serviceKey, TimeUnit.SECONDS);

        Long scheduleTtl = redisTemplate.getExpire(scheduleKey, TimeUnit.SECONDS);

        assertNotNull(serviceTtl);
        assertNotNull(scheduleTtl);

        assertTrue(
                serviceTtl > 0 && serviceTtl <= 600,
                "Incorrect schoolServices TTL: " + serviceTtl
        );

        assertTrue(
                scheduleTtl > 0 && scheduleTtl <= 30,
                "Incorrect trainingSchedule TTL: " + scheduleTtl
        );
    }
}
