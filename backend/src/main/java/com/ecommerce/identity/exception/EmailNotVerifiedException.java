package com.ecommerce.identity.exception;

public class EmailNotVerifiedException extends AuthenticationFailureException {

    public EmailNotVerifiedException() {
        super("Email verification is pending");
    }
}
