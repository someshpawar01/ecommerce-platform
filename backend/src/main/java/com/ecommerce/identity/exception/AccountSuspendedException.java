package com.ecommerce.identity.exception;

public class AccountSuspendedException extends AuthenticationFailureException {

    public AccountSuspendedException() {
        super("Account is suspended");
    }
}
