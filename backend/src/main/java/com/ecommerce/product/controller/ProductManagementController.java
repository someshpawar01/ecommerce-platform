package com.ecommerce.product.controller;

import com.ecommerce.product.dto.request.AssignProductCategoriesRequest;
import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.request.UpdateProductRequest;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.service.ProductManagementService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
public class ProductManagementController {

    private final ProductManagementService productManagementService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productManagementService.createProduct(request);

        URI location = URI.create("/api/v1/admin/products/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productManagementService.getProduct(productId));
    }

    @PatchMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
        @PathVariable Long productId, @Valid @RequestBody UpdateProductRequest request) {

        return ResponseEntity.ok(productManagementService.updateProduct(productId, request));
    }

    @PutMapping("/{productId}/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> assignCategories(
        @PathVariable Long productId, @Valid @RequestBody AssignProductCategoriesRequest request) {

        return ResponseEntity.ok(productManagementService.assignCategories(productId, request));
    }
}
