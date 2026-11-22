package com.ecommerce.product.controller;

import com.ecommerce.product.dto.request.CreateCategoryRequest;
import com.ecommerce.product.dto.request.UpdateCategoryRequest;
import com.ecommerce.product.dto.request.UpdateCategoryStatusRequest;
import com.ecommerce.product.dto.response.CategoryResponse;
import com.ecommerce.product.service.CategoryManagementService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/categories")
public class CategoryManagementController {

    private final CategoryManagementService categoryManagementService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = categoryManagementService.createCategory(request);
        URI location = URI.create("/api/v1/categories/" + response.slug());

        return ResponseEntity.created(location).body(response);
    }

    //for sortOrder update we are just modifying current category
    //so 2 cats can have same sortOrder for now.
    @PatchMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
        @PathVariable Long categoryId, @Valid @RequestBody UpdateCategoryRequest request) {

        return ResponseEntity.ok(categoryManagementService.updateCategory(categoryId, request));
    }

    @PatchMapping("/{categoryId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategoryStatus(
        @PathVariable Long categoryId, @Valid @RequestBody UpdateCategoryStatusRequest request) {

        return ResponseEntity.ok(categoryManagementService.setCategoryActive(categoryId, request.active()));
    }
}
