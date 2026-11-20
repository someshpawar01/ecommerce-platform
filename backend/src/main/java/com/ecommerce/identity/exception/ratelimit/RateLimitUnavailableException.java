package com.ecommerce.identity.exception.ratelimit;

public class RateLimitUnavailableException extends RuntimeException {

    public RateLimitUnavailableException(Throwable cause) {
        super("Rate-limit service unavailable", cause);
    }
}
