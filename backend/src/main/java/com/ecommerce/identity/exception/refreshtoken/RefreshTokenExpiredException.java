package com.ecommerce.identity.exception.refreshtoken;

import com.ecommerce.identity.exception.AuthenticationFailureException;

public class RefreshTokenExpiredException extends AuthenticationFailureException {

    public RefreshTokenExpiredException() {
        super("Refresh token has expired");
    }
}
