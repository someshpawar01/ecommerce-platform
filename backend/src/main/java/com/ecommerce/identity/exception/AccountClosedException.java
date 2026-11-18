package com.ecommerce.identity.exception;

public class AccountClosedException extends AuthenticationFailureException {

    public AccountClosedException() {
        super("Account is closed");
    }
}
