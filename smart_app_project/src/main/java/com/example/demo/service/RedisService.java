package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis에 데이터를 저장하고 유효시간(분)을 설정합니다.
     * @param key     (예: "email:test@example.com")
     * @param value   (예: "123456")
     * @param minutes 유효시간 (분 단위)
     */
    public void setData(String key, String value, long minutes) {
            // Duration 객체를 사용해 유효시간 설정
            Duration duration = Duration.ofMinutes(minutes);
            // opsForValue()는 Redis의 String 타입 데이터를 다룰 때 사용
            redisTemplate.opsForValue().set(key, value, duration);
    }

    /**
     * Redis에서 키에 해당하는 값을 가져옵니다.
     * @param key (예: "email:test@example.com")
     * @return 저장된 값 (없거나 만료되면 null 반환)
     */
    public String getData(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Redis에서 데이터를 삭제합니다. (인증 성공 시 사용)
     * @param key (예: "email:test@example.com")
     */
    public void deleteData(String key) {
        redisTemplate.delete(key);
    }
}
