package com.ecommerce.identity.controller;

import com.ecommerce.identity.dto.request.VerifyEmailRequest;
import com.ecommerce.identity.dto.response.VerifyEmailResponse;
import com.ecommerce.identity.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

  private final EmailVerificationService emailVerificationService;

  @PostMapping("/verify-email")
  public ResponseEntity<VerifyEmailResponse> verifyEmail(
      @Valid @RequestBody VerifyEmailRequest request) {
    emailVerificationService.verifyEmail(request.token());

    return ResponseEntity.ok(new VerifyEmailResponse("Email verified successfully."));
  }
}
