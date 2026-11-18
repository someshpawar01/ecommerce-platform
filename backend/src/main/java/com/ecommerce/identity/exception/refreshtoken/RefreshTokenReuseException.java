package com.ecommerce.identity.exception.refreshtoken;

import com.ecommerce.identity.exception.AuthenticationFailureException;

public class RefreshTokenReuseException extends AuthenticationFailureException {

    public RefreshTokenReuseException() {
        super("Refresh token reuse detected");
    }
}
