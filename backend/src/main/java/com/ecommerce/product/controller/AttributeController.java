package com.ecommerce.product.controller;

import com.ecommerce.product.dto.response.AttributeResponse;
import com.ecommerce.product.dto.response.AttributeValueResponse;
import com.ecommerce.product.service.AttributeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/attributes")
public class AttributeController {

    private final AttributeService attributeService;

    @GetMapping
    public ResponseEntity<List<AttributeResponse>> getAttributes() {
        return ResponseEntity.ok(attributeService.getAttributes());
    }

    @GetMapping("/{attributeId}")
    public ResponseEntity<AttributeResponse> getAttribute(@PathVariable Long attributeId) {
        return ResponseEntity.ok(attributeService.getAttribute(attributeId));
    }

    @GetMapping("/{attributeId}/values")
    public ResponseEntity<List<AttributeValueResponse>> getAttributeValues(@PathVariable Long attributeId) {
        return ResponseEntity.ok(attributeService.getAttributeValues(attributeId));
    }
}
