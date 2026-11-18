package com.ecommerce.identity.controller;

import com.ecommerce.identity.dto.request.RegisterCustomerRequest;
import com.ecommerce.identity.dto.response.RegisterCustomerResponse;
import com.ecommerce.identity.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class RegistrationController {

  private final RegistrationService registrationService;

  @PostMapping("/register")
  public ResponseEntity<RegisterCustomerResponse> register(
      @Valid @RequestBody RegisterCustomerRequest request) {

    RegisterCustomerResponse response = registrationService.registerCustomer(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
