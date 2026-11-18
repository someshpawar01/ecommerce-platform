package com.ecommerce.identity.exception.refreshtoken;

import com.ecommerce.identity.exception.AuthenticationFailureException;

public class InvalidRefreshTokenException extends AuthenticationFailureException {

    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }
}
