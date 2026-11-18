package com.ecommerce.identity.utils;

import com.ecommerce.identity.exception.InvalidPasswordException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

    private static final int MAX_PASSWORD_BYTES = 72;

    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new InvalidPasswordException("Password must not be blank");
        }

        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;

        if (passwordBytes > MAX_PASSWORD_BYTES) {
            throw new InvalidPasswordException("Password must not exceed 72 UTF-8 bytes");
        }
    }
}
