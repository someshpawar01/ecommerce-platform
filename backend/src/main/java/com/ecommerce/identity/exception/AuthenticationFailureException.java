package com.ecommerce.identity.exception;

public abstract class AuthenticationFailureException extends RuntimeException {

    protected AuthenticationFailureException(String message) {
        super(message);
    }
}
