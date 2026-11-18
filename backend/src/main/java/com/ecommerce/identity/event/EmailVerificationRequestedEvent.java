package com.ecommerce.identity.event;

public record EmailVerificationRequestedEvent(String email, String rawToken) {}
