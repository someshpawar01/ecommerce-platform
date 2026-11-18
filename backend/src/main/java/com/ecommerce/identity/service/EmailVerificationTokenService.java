package com.ecommerce.identity.service;

import com.ecommerce.identity.entity.User;

public interface EmailVerificationTokenService {

  void issueVerificationToken(User user);
}
