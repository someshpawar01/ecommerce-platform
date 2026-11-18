package com.ecommerce.identity.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(
    Duration tokenExpiry,
    String baseUrl
) {
}
