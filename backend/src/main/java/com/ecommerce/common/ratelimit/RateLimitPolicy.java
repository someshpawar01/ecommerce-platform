package com.ecommerce.common.ratelimit;

public record RateLimitPolicy(String name, int limit, long windowSeconds) {

    public RateLimitPolicy {
        if (name == null || name.isBlank() || limit <= 0 || windowSeconds <= 0) {
            throw new IllegalArgumentException("Invalid rate-limit policy");
        }
    }
}
