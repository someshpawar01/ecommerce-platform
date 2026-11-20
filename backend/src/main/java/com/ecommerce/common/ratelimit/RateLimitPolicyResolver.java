package com.ecommerce.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class RateLimitPolicyResolver {

    public RateLimitPolicy resolve(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getServletPath();

        if (!HttpMethod.POST.matches(method)) {
            return null;
        }

        return switch (path) {
            case "/api/v1/auth/login" -> new RateLimitPolicy("login", 10, 60);
            case "/api/v1/auth/register" -> new RateLimitPolicy("register", 5, 600);
            case "/api/v1/auth/forgot-password" -> new RateLimitPolicy("forgot-password", 10, 900);
            case "/api/v1/auth/reset-password" -> new RateLimitPolicy("reset-password", 10, 600);
            case "/api/v1/auth/resend-verification" -> new RateLimitPolicy("resend-verification", 10, 900);
            case "/api/v1/auth/verify-email" -> new RateLimitPolicy("verify-email", 20, 600);
            case "/api/v1/auth/refresh" -> new RateLimitPolicy("refresh", 60, 60);
            default -> null;
        };
    }
}
