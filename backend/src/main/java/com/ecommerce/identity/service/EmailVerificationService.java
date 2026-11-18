package com.ecommerce.identity.service;

public interface EmailVerificationService {

  void verifyEmail(String rawToken);
}
