package com.ecommerce.identity.controller;

import com.ecommerce.identity.dto.request.ResendVerificationRequest;
import com.ecommerce.identity.service.ResendVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class ResendVerificationController {

  private final ResendVerificationService resendVerificationService;

  @PostMapping("/resend-verification")
  public ResponseEntity<Void> resendVerification(
      @Valid @RequestBody ResendVerificationRequest request) {
    resendVerificationService.resendVerification(request.email());

    return ResponseEntity.accepted().build();
  }
}
