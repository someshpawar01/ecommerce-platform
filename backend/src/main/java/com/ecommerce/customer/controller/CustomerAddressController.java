package com.ecommerce.customer.controller;

import com.ecommerce.customer.dto.request.CreateCustomerAddressRequest;
import com.ecommerce.customer.dto.request.UpdateCustomerAddressRequest;
import com.ecommerce.customer.dto.response.CustomerAddressResponse;
import com.ecommerce.customer.service.CustomerAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers/me/addresses")
public class CustomerAddressController {

    private final CustomerAddressService customerAddressService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerAddressResponse> createAddress(
        @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateCustomerAddressRequest request) {

        Long customerUserId = Long.valueOf(jwt.getSubject());

        CustomerAddressResponse response = customerAddressService.createAddress(customerUserId, request);

        URI location = URI.create("/api/v1/customers/me/addresses/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<CustomerAddressResponse>> getMyAddresses(@AuthenticationPrincipal Jwt jwt) {
        Long customerUserId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(customerAddressService.getMyAddresses(customerUserId));
    }

    @GetMapping("/{addressId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerAddressResponse> getMyAddress(
        @AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId) {

        Long customerUserId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(customerAddressService.getMyAddress(customerUserId, addressId));
    }

    @PatchMapping("/{addressId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerAddressResponse> updateAddress(
        @AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId,
        @Valid @RequestBody UpdateCustomerAddressRequest request) {

        Long customerUserId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(customerAddressService.updateAddress(customerUserId, addressId, request));
    }

    @PutMapping("/{addressId}/default")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerAddressResponse> setDefaultAddress(
        @AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId) {

        Long customerUserId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(customerAddressService.setDefaultAddress(customerUserId, addressId));
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId) {

        Long customerUserId = Long.valueOf(jwt.getSubject());

        customerAddressService.deleteAddress(customerUserId, addressId);

        return ResponseEntity.noContent().build();
    }
}
