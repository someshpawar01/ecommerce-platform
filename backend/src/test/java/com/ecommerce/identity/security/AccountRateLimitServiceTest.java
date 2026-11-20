package com.ecommerce.identity.security;

import com.ecommerce.common.ratelimit.RedisRateLimiter;
import com.ecommerce.identity.exception.ratelimit.AccountRateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountRateLimitServiceTest {

    private RedisRateLimiter redisRateLimiter;
    private AccountRateLimitService accountRateLimitService;

    @BeforeEach
    void setUp() {
        redisRateLimiter = mock(RedisRateLimiter.class);

        accountRateLimitService = new AccountRateLimitService(
            redisRateLimiter,
            "test-only-secret-key-with-sufficient-length-123");
    }

    @Test
    void shouldAllowLoginWhenAccountIsWithinLimit() {
        when(redisRateLimiter.allow(anyString(), eq(5), eq(60L))).thenReturn(true);

        assertDoesNotThrow(() -> accountRateLimitService.checkLogin("customer@example.com"));

        verify(redisRateLimiter).allow(
            startsWith("ecommerce:ratelimit:login:email:"),
            eq(5),
            eq(60L));
    }

    @Test
    void shouldRejectLoginWhenAccountLimitIsExceeded() {
        when(redisRateLimiter.allow(anyString(), eq(5), eq(60L))).thenReturn(false);

        assertThrows(
            AccountRateLimitExceededException.class,
            () -> accountRateLimitService.checkLogin("customer@example.com"));
    }

    @Test
    void shouldUseSameKeyForSameEmail() {
        when(redisRateLimiter.allow(anyString(), anyInt(), anyLong())).thenReturn(true);

        accountRateLimitService.checkLogin("customer@example.com");
        accountRateLimitService.checkLogin("customer@example.com");

        var keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

        verify(redisRateLimiter, times(2))
            .allow(keyCaptor.capture(), eq(5), eq(60L));

        assertEquals(keyCaptor.getAllValues().get(0), keyCaptor.getAllValues().get(1));
    }

    @Test
    void shouldUseDifferentKeysForDifferentEmails() {
        when(redisRateLimiter.allow(anyString(), anyInt(), anyLong())).thenReturn(true);

        accountRateLimitService.checkLogin("customer1@example.com");
        accountRateLimitService.checkLogin("customer2@example.com");

        var keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

        verify(redisRateLimiter, times(2))
            .allow(keyCaptor.capture(), eq(5), eq(60L));

        assertNotEquals(keyCaptor.getAllValues().get(0), keyCaptor.getAllValues().get(1));
    }

    @Test
    void shouldApplyForgotPasswordLimit() {
        when(redisRateLimiter.allow(anyString(), eq(3), eq(900L))).thenReturn(true);

        accountRateLimitService.checkForgotPassword("customer@example.com");

        verify(redisRateLimiter).allow(
            startsWith("ecommerce:ratelimit:forgot-password:email:"),
            eq(3),
            eq(900L));
    }
}
