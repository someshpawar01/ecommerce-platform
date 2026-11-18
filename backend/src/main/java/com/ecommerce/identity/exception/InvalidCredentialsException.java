package com.ecommerce.identity.exception;

public class InvalidCredentialsException extends AuthenticationFailureException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
