package com.ecommerce.identity.exception.refreshtoken;

import com.ecommerce.identity.exception.AuthenticationFailureException;

public class SessionRevokedException extends AuthenticationFailureException {

    public SessionRevokedException() {
        super("Login session has been revoked");
    }
}
