package com.ecommerce.product.controller;

import com.ecommerce.product.dto.request.UpdateProductVariantRequest;
import com.ecommerce.product.dto.request.UpdateProductVariantStatusRequest;
import com.ecommerce.product.dto.response.ProductVariantResponse;
import com.ecommerce.product.service.ProductVariantManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/variants")
@PreAuthorize("hasRole('ADMIN')")
public class VariantManagementController {

    private final ProductVariantManagementService variantService;

    @GetMapping("/{variantId}")
    public ResponseEntity<ProductVariantResponse> getVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(variantService.getVariant(variantId));
    }

    @PatchMapping("/{variantId}")
    public ResponseEntity<ProductVariantResponse> updateVariant(
        @PathVariable Long variantId, @Valid @RequestBody UpdateProductVariantRequest request) {

        return ResponseEntity.ok(variantService.updateVariant(variantId, request));
    }

    @PatchMapping("/{variantId}/status")
    public ResponseEntity<ProductVariantResponse> updateVariantStatus(
        @PathVariable Long variantId, @Valid @RequestBody UpdateProductVariantStatusRequest request) {

        return ResponseEntity.ok(variantService.setVariantActive(variantId, request.active()));
    }
}
