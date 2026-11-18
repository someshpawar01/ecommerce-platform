package com.ecommerce.identity.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({EmailVerificationProperties.class, PasswordResetProperties.class})
public class IdentityConfig {
}
