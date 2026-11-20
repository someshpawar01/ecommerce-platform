package com.ecommerce.customer.controller;

import com.ecommerce.customer.dto.request.UpdateCustomerProfileRequest;
import com.ecommerce.customer.dto.response.CustomerProfileResponse;
import com.ecommerce.customer.service.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(customerProfileService.getMyProfile(userId));
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerProfileResponse> updateMyProfile(
        @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateCustomerProfileRequest request) {

        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(customerProfileService.updateMyProfile(userId, request));
    }
}
