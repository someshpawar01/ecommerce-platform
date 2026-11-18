package com.ecommerce.identity.event;

public record PasswordResetRequestedEvent(
    String email,
    String rawToken
) {
}
