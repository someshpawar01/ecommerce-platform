package com.ecommerce.identity.exception.ratelimit;

public class AccountRateLimitExceededException extends RuntimeException {

    public AccountRateLimitExceededException() {
        super("Too many requests");
    }
}
