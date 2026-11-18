package com.ecommerce.identity.exception;

public class AccountNotActiveException extends AuthenticationFailureException {

    public AccountNotActiveException() {
        super("Account is closed");
    }
}
