package com.ecommerce.identity.service;

public interface VerificationEmailSender {

  void sendVerificationEmail(String email, String verificationLink);
}
