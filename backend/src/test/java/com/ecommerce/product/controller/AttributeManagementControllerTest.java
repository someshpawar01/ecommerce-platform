package com.ecommerce.product.controller;

import com.ecommerce.product.dto.request.CreateAttributeRequest;
import com.ecommerce.product.dto.request.CreateAttributeValueRequest;
import com.ecommerce.product.dto.request.UpdateAttributeRequest;
import com.ecommerce.product.dto.request.UpdateAttributeValueRequest;
import com.ecommerce.product.dto.response.AttributeResponse;
import com.ecommerce.product.dto.response.AttributeValueResponse;
import com.ecommerce.product.service.AttributeManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttributeManagementControllerTest {

    @Mock
    private AttributeManagementService attributeManagementService;

    @InjectMocks
    private AttributeManagementController controller;

    @Test
    void shouldCreateAttribute() {
        CreateAttributeRequest request = new CreateAttributeRequest("Color");
        AttributeResponse attribute = new AttributeResponse(1L, "color", "Color");

        when(attributeManagementService.createAttribute(request)).thenReturn(attribute);

        ResponseEntity<AttributeResponse> response = controller.createAttribute(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("/api/v1/attributes/1", response.getHeaders().getLocation().toString());
        assertEquals("Color", response.getBody().name());
    }

    @Test
    void shouldUpdateAttribute() {
        UpdateAttributeRequest request = new UpdateAttributeRequest("Product Color");
        AttributeResponse attribute = new AttributeResponse(1L, "color", "Product Color");

        when(attributeManagementService.updateAttribute(1L, request)).thenReturn(attribute);

        ResponseEntity<AttributeResponse> response = controller.updateAttribute(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Product Color", response.getBody().name());
    }

    @Test
    void shouldCreateAttributeValue() {
        CreateAttributeValueRequest request = new CreateAttributeValueRequest("Black");
        AttributeValueResponse value = new AttributeValueResponse(10L, 1L, "Black", 0);

        when(attributeManagementService.createAttributeValue(1L, request)).thenReturn(value);

        ResponseEntity<AttributeValueResponse> response = controller.createAttributeValue(1L, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Black", response.getBody().value());
        assertEquals("/api/v1/attributes/1/values", response.getHeaders().getLocation().toString());
    }

    @Test
    void shouldUpdateAttributeValue() {
        UpdateAttributeValueRequest request = new UpdateAttributeValueRequest("Midnight Black");
        AttributeValueResponse value = new AttributeValueResponse(10L, 1L, "Midnight Black", 0);

        when(attributeManagementService.updateAttributeValue(10L, request)).thenReturn(value);

        ResponseEntity<AttributeValueResponse> response = controller.updateAttributeValue(10L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Midnight Black", response.getBody().value());
    }
}
