
package com.skating.platform.payment.integration;

import com.skating.platform.payment.entity.OutboxEvent;
import com.skating.platform.payment.outbox.OutboxEventPublisher;
import com.skating.platform.payment.repository.OutboxEventRepository;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import org.apache.kafka.common.serialization.StringDeserializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.errors.TopicExistsException;
import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "payment.outbox.fixed-delay=3600000",
        "payment.reconciliation.enabled=false",
        "spring.kafka.properties.security.protocol=PLAINTEXT",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
@Testcontainers
class OutboxEventIntegrationTest {

    private static final String TOPIC = "payments.requested";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.3.2"));

    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("KAFKA_APP_USERNAME", () -> "test-app");
        registry.add("KAFKA_APP_PASSWORD", () -> "test-password");
        registry.add("KAFKA_SSL_STORE_PASSWORD", () -> "test-store-password");
    }

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void publishPendingEvent_shouldSendMessageToKafkaAndMarkAsPublished()
            throws Exception {

        String aggregateId = "payment-1001";

        String payload = """
                {
                    "paymentId": 1001,
                    "orderId": 500,
                    "amount": 1500.00,
                    "currency": "RUB",
                    "status": "PENDING"
                }
                """;


        OutboxEvent event = new OutboxEvent();

        event.setAggregateType("PAYMENT");
        event.setAggregateId(aggregateId);
        event.setEventType("PAYMENT_REQUESTED");
        event.setPayload(payload);

        OutboxEvent savedEvent =
                outboxEventRepository.saveAndFlush(event);


        try (AdminClient adminClient = AdminClient.create(
                Map.of(
                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                        kafka.getBootstrapServers()
                )
        )) {
            try {
                adminClient.createTopics(
                        List.of(new NewTopic(TOPIC, 1, (short) 1))
                ).all().get(10, TimeUnit.SECONDS);

            } catch (ExecutionException exception) {
                if (!(exception.getCause() instanceof TopicExistsException)) {
                    throw exception;
                }
            }
        }

        Properties consumerProperties = new Properties();

        consumerProperties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers()
        );

        consumerProperties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "outbox-integration-" + UUID.randomUUID()
        );

        consumerProperties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        consumerProperties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName()
        );

        consumerProperties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName()
        );

        consumerProperties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        consumerProperties.put(
                "security.protocol",
                "PLAINTEXT"
        );

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties)) {

            consumer.subscribe(List.of(TOPIC));


            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();

            while (consumer.assignment().isEmpty() && System.nanoTime() < deadline) {
                consumer.poll(Duration.ofMillis(100));
            }

            assertFalse(
                    consumer.assignment().isEmpty(),
                    "Kafka consumer was not assigned a partition"
            );


            outboxEventPublisher.publishPendingEvents();


            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));

            assertEquals(1, records.count());

            ConsumerRecord<String, String> message = records.iterator().next();

            assertEquals(aggregateId, message.key());
            assertEquals(payload, message.value());
        }


        OutboxEvent publishedEvent = outboxEventRepository.findById(savedEvent.getId())
                        .orElseThrow();

        assertNotNull(publishedEvent.getPublishedAt());

        assertNull(publishedEvent.getLastError());

        assertEquals(0, publishedEvent.getAttempts());
    }
}
