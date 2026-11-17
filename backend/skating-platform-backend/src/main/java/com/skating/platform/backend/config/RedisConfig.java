
package com.skating.platform.backend.config;

import com.skating.platform.backend.dto.cache.CachedPage;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory
    ) {

        BasicPolymorphicTypeValidator typeValidator =
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.skating.platform.backend.dto.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.math.")
                        .build();

        RedisSerializer<Object> jsonSerializer =
                GenericJacksonJsonRedisSerializer.builder()
                        .enableDefaultTyping(typeValidator)
                        .build();

        RedisSerializer<Object> valueSerializer =
                new PageAwareRedisSerializer(jsonSerializer);

        RedisCacheConfiguration defaultConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .disableCachingNullValues()
                        .computePrefixWith(
                                cacheName ->
                                        "skating-platform::" + cacheName + "::"
                        )
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(
                                                new StringRedisSerializer()
                                        )
                        )
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(valueSerializer)
                        );

        // Создаём RedisCacheWriter с синхронными операциями
        RedisCacheWriter cacheWriter = RedisCacheWriter.create(
                connectionFactory,
                configurer -> configurer.immediateWrites()
        );

        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(
                        defaultConfig.entryTtl(Duration.ofMinutes(5))
                )
                .withCacheConfiguration(
                        "schoolServices",
                        defaultConfig.entryTtl(Duration.ofMinutes(10))
                )
                .withCacheConfiguration(
                        "trainingSchedule",
                        defaultConfig.entryTtl(Duration.ofSeconds(30))
                )
                .build();
    }

    private static class PageAwareRedisSerializer
            implements RedisSerializer<Object> {

        private final RedisSerializer<Object> delegate;

        private PageAwareRedisSerializer(
                RedisSerializer<Object> delegate
        ) {
            this.delegate = delegate;
        }

        @Override
        public byte[] serialize(Object value) {

            if (value instanceof Page<?> page) {
                value = CachedPage.from(page);
            }

            return delegate.serialize(value);
        }

        @Override
        public Object deserialize(byte[] bytes) {

            Object value = delegate.deserialize(bytes);

            if (value instanceof CachedPage cachedPage) {
                return cachedPage.toPage();
            }

            return value;
        }
    }
}
