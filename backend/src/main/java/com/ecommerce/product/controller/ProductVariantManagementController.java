package com.ecommerce.product.controller;

import com.ecommerce.product.dto.request.CreateProductVariantRequest;
import com.ecommerce.product.dto.response.ProductVariantResponse;
import com.ecommerce.product.service.ProductVariantManagementService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products/{productId}/variants")
@PreAuthorize("hasRole('ADMIN')")
public class ProductVariantManagementController {

    private final ProductVariantManagementService variantService;

    @PostMapping
    public ResponseEntity<ProductVariantResponse> createVariant(
        @PathVariable Long productId, @Valid @RequestBody CreateProductVariantRequest request) {

        ProductVariantResponse response = variantService.createVariant(productId, request);

        return ResponseEntity.created(URI.create("/api/v1/admin/variants/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductVariantResponse>> getProductVariants(@PathVariable Long productId) {
        return ResponseEntity.ok(variantService.getProductVariants(productId));
    }
}
