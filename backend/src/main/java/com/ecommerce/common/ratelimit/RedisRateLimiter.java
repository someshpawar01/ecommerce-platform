package com.ecommerce.common.ratelimit;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> rateLimitScript;

    public boolean allow(String key, int limit, long windowSeconds) {
        Long result = redisTemplate.execute(
            rateLimitScript,
            List.of(key),
            String.valueOf(limit),
            String.valueOf(windowSeconds)
        );

        if (result == null) {
            throw new IllegalStateException("Redis rate limiter returned no result");
        }

        return result == 1L;
    }
}
