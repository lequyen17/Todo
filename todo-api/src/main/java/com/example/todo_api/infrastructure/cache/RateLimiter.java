package com.example.todo_api.infrastructure.cache;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.core.exceptions.CommonErrorCode;

/** Fixed window theo key rate:{ip|email}:{endpoint}, cửa sổ 1 phút. */
@Component
public class RateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redis;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void check(String subject, String endpoint, int limit) {
        String key = "rate:" + subject + ":" + endpoint;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, WINDOW);
        }
        if (count != null && count > limit) {
            throw new ApiException(CommonErrorCode.RATE_LIMITED);
        }
    }
}
