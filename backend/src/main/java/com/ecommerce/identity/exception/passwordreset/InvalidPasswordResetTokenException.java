package com.ecommerce.identity.exception.passwordreset;

public class InvalidPasswordResetTokenException extends RuntimeException {

    public InvalidPasswordResetTokenException() {
        super("Invalid or expired password reset token");
    }
}
