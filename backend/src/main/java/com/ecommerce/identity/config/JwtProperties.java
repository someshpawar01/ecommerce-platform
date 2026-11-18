package com.ecommerce.identity.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
    @NotBlank String issuer,
    @NotBlank String audience,
    @Min(1) @Max(15) long accessTokenMinutes,
    @NotBlank String keyId) {}
