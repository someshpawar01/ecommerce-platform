package com.ecommerce.common.ratelimit;

import com.ecommerce.common.security.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitPolicyResolver policyResolver;
    private final RedisRateLimiter redisRateLimiter;
    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return CorsUtils.isPreFlightRequest(request) || policyResolver.resolve(request) == null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        RateLimitPolicy policy = policyResolver.resolve(request);
        //gets the client IP ; need to implement Correct IP identification behind gateways
        String clientIp = request.getRemoteAddr();

        String redisKey = "ecommerce:ratelimit:" + policy.name() + ":ip:" + clientIp;

        try {
            boolean allowed = redisRateLimiter.allow(redisKey, policy.limit(), policy.windowSeconds());

            if (!allowed) {
                errorResponseWriter.write(
                    request,
                    response,
                    HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests. Please try again later."
                );
                return;
            }

        } catch (DataAccessException exception) {
            errorResponseWriter.write(
                request,
                response,
                HttpStatus.SERVICE_UNAVAILABLE,
                "SERVICE_UNAVAILABLE",
                "The service is temporarily unavailable. Please try again later."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
