package com.example.demo.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
// @EnableCaching은 application class에 있거나, 별도 config에 명시되어야 합니다.
public class RedisConfig {

    // application.properties에서 호스트, 포트 정보를 가져옴
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    // ⭐️ 누락된 Redis 인증 정보 변수 추가 ⭐️
    @Value("${spring.data.redis.username}")
    private String username;

    @Value("${spring.data.redis.password}")
    private String password;

    // (application.properties에 사용자 이름/비밀번호가 있으므로, LettuceConnectionFactory는 이를 자동 처리할 수 있습니다.)

    // 1. Redis와 연결을 위한 ConnectionFactory 빈 생성
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // RedisStandaloneConfiguration을 사용하여 Host, Port, Username, Password를 모두 명시적으로 설정해야 합니다.
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        config.setUsername(username);
        config.setPassword(password); // ⬅️ 이 부분이 NOAUTH 오류 해결에 필수입니다.

        return new LettuceConnectionFactory(config);
    }

    // 2. Redis 데이터를 다루기 위한 RedisTemplate 빈 생성 (Redis CLI에서 보기 쉽게 String 설정)
    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        // Key-Value 데이터를 문자열(String)로 직렬화(Serialize)하도록 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

    // 3. CacheManager 빈 생성 (Redis를 캐시 시스템으로 사용하도록 명시)
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 기본 캐시 설정을 정의합니다.
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 키를 문자열로 직렬화
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 값을 JSON으로 직렬화 (GenericJackson2JsonRedisSerializer를 StringRedisSerializer로 변경하여 String으로 저장)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 캐시 만료 시간 (3분 설정 - EmailService 로직과 일치)
                .entryTtl(Duration.ofMinutes(3));

        // RedisCacheManager를 빌드하여 반환합니다.
        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(connectionFactory)
                .cacheDefaults(config)
                // emailAuthCodes 캐시를 명시적으로 설정 (선택 사항)
                .withCacheConfiguration("emailAuthCodes", config)
                .build();
    }
}