package com.ecommerce;

import com.ecommerce.common.ratelimit.RedisRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RedisRateLimiterIntegrationTest.RedisTestConfig.class)
class RedisRateLimiterIntegrationTest {

    @Configuration(proxyBeanMethods = false)
    static class RedisTestConfig {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory("localhost", 6379);
        }

        @Bean
        StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
            return new StringRedisTemplate(connectionFactory);
        }

        @Bean
        DefaultRedisScript<Long> rateLimitScript() {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setLocation(new ClassPathResource("redis/rate-limit.lua"));
            script.setResultType(Long.class);
            return script;
        }

        @Bean
        RedisRateLimiter redisRateLimiter(StringRedisTemplate redisTemplate,
                                          DefaultRedisScript<Long> rateLimitScript) {
            return new RedisRateLimiter(redisTemplate, rateLimitScript);
        }
    }

    @Autowired
    private RedisRateLimiter redisRateLimiter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldRejectRequestsAfterLimitIsExceeded() {
        String key = "ecommerce:test:ratelimit:" + UUID.randomUUID();

        try {
            assertTrue(redisRateLimiter.allow(key, 3, 60));
            assertTrue(redisRateLimiter.allow(key, 3, 60));
            assertTrue(redisRateLimiter.allow(key, 3, 60));

            assertFalse(redisRateLimiter.allow(key, 3, 60));
            assertFalse(redisRateLimiter.allow(key, 3, 60));

            assertEquals("3", redisTemplate.opsForValue().get(key));
        } finally {
            redisTemplate.delete(key);
        }
    }

    @Test
    void shouldApplyExpirationToCounter() {
        String key = "ecommerce:test:ratelimit:" + UUID.randomUUID();

        try {
            assertTrue(redisRateLimiter.allow(key, 5, 60));

            Long ttl = redisTemplate.getExpire(key);

            assertNotNull(ttl);
            assertTrue(ttl > 0);
            assertTrue(ttl <= 60);
        } finally {
            redisTemplate.delete(key);
        }
    }
}
