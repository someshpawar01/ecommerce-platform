package com.ecommerce.identity.service;

public interface PasswordResetEmailSender {

    void sendPasswordResetEmail(String email, String resetLink);
}
