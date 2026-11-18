package com.ecommerce.identity.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.verification")
public record EmailVerificationProperties(
    Duration tokenExpiry, String baseUrl, Duration resendCooldown, int maxTokensPerDay) {}
