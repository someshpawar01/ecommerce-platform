package com.ecommerce.product.controller;

import com.ecommerce.product.dto.request.CreateBrandRequest;
import com.ecommerce.product.dto.request.UpdateBrandRequest;
import com.ecommerce.product.dto.request.UpdateBrandStatusRequest;
import com.ecommerce.product.dto.response.BrandResponse;
import com.ecommerce.product.service.BrandManagementService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/brands")
public class BrandManagementController {

    private final BrandManagementService brandManagementService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> createBrand(@Valid @RequestBody CreateBrandRequest request) {
        BrandResponse response = brandManagementService.createBrand(request);
        URI location = URI.create("/api/v1/brands/" + response.slug());

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{brandId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> updateBrand(
        @PathVariable Long brandId, @Valid @RequestBody UpdateBrandRequest request) {

        return ResponseEntity.ok(brandManagementService.updateBrand(brandId, request));
    }

    @PatchMapping("/{brandId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> updateBrandStatus(
        @PathVariable Long brandId, @Valid @RequestBody UpdateBrandStatusRequest request) {

        return ResponseEntity.ok(brandManagementService.setBrandActive(brandId, request.active()));
    }
}
