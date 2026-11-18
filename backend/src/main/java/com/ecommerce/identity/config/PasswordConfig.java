package com.ecommerce.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

  // Using PasswordEncoder here provides Dependency Inversion
  // As in future if we replace BCryptPasswordEncoder with
  // some other hashing algo then the service will depend on PasswordEncoder
  @Bean
  public PasswordEncoder passwordEncoder() {
    //12 is Bcrypt's work factor, also called its cost parameter; exponentially increases
    return new BCryptPasswordEncoder(12);
  }
}
