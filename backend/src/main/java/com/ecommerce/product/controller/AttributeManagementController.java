package com.ecommerce.product.controller;

import com.ecommerce.product.dto.request.CreateAttributeRequest;
import com.ecommerce.product.dto.request.CreateAttributeValueRequest;
import com.ecommerce.product.dto.request.UpdateAttributeRequest;
import com.ecommerce.product.dto.request.UpdateAttributeValueRequest;
import com.ecommerce.product.dto.response.AttributeResponse;
import com.ecommerce.product.dto.response.AttributeValueResponse;
import com.ecommerce.product.service.AttributeManagementService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AttributeManagementController {

    private final AttributeManagementService attributeManagementService;

    @PostMapping("/attributes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttributeResponse> createAttribute(@Valid @RequestBody CreateAttributeRequest request) {
        AttributeResponse response = attributeManagementService.createAttribute(request);

        return ResponseEntity.created(URI.create("/api/v1/attributes/" + response.id())).body(response);
    }

    @PatchMapping("/attributes/{attributeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttributeResponse> updateAttribute(
        @PathVariable Long attributeId, @Valid @RequestBody UpdateAttributeRequest request) {

        return ResponseEntity.ok(attributeManagementService.updateAttribute(attributeId, request));
    }

    @PostMapping("/attributes/{attributeId}/values")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttributeValueResponse> createAttributeValue(
        @PathVariable Long attributeId, @Valid @RequestBody CreateAttributeValueRequest request) {

        AttributeValueResponse response = attributeManagementService.createAttributeValue(attributeId, request);

        return ResponseEntity.created(
            URI.create("/api/v1/attributes/" + attributeId + "/values")).body(response);
    }

    @PatchMapping("/attribute-values/{valueId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttributeValueResponse> updateAttributeValue(
        @PathVariable Long valueId, @Valid @RequestBody UpdateAttributeValueRequest request) {

        return ResponseEntity.ok(attributeManagementService.updateAttributeValue(valueId, request));
    }
}
