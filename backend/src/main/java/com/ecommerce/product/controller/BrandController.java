package com.ecommerce.product.controller;

import com.ecommerce.product.dto.response.BrandResponse;
import com.ecommerce.product.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/brands")
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<List<BrandResponse>> getActiveBrands() {
        return ResponseEntity.ok(brandService.getActiveBrands());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<BrandResponse> getBrandBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(brandService.getBrandBySlug(slug));
    }
}
