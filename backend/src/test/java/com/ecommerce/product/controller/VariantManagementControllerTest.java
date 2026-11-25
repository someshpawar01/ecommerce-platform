package com.ecommerce.product.controller;

import com.ecommerce.product.dto.request.UpdateProductVariantRequest;
import com.ecommerce.product.dto.request.UpdateProductVariantStatusRequest;
import com.ecommerce.product.dto.response.ProductVariantResponse;
import com.ecommerce.product.service.ProductVariantManagementService;
import java.math.BigDecimal;
import java.util.List;
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
class VariantManagementControllerTest {

    @Mock
    private ProductVariantManagementService variantService;

    @InjectMocks
    private VariantManagementController controller;

    @Test
    void shouldGetVariant() {
        ProductVariantResponse variant = variantResponse(true);

        when(variantService.getVariant(201L)).thenReturn(variant);

        ResponseEntity<ProductVariantResponse> response = controller.getVariant(201L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(201L, response.getBody().id());
        assertEquals("SAM-S25-8-BLK", response.getBody().sku());

        verify(variantService).getVariant(201L);
    }

    @Test
    void shouldUpdateVariant() {
        UpdateProductVariantRequest request = new UpdateProductVariantRequest(
            "UPDATED-SKU", "Updated variant", new BigDecimal("75999.00"), null, 170);

        ProductVariantResponse variant = variantResponse(true);

        when(variantService.updateVariant(201L, request)).thenReturn(variant);

        ResponseEntity<ProductVariantResponse> response = controller.updateVariant(201L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(variantService).updateVariant(201L, request);
    }

    @Test
    void shouldDeactivateVariant() {
        UpdateProductVariantStatusRequest request = new UpdateProductVariantStatusRequest(false);
        ProductVariantResponse variant = variantResponse(false);

        when(variantService.setVariantActive(201L, false)).thenReturn(variant);

        ResponseEntity<ProductVariantResponse> response = controller.updateVariantStatus(201L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().active());

        verify(variantService).setVariantActive(201L, false);
    }

    @Test
    void shouldReactivateVariant() {
        UpdateProductVariantStatusRequest request = new UpdateProductVariantStatusRequest(true);
        ProductVariantResponse variant = variantResponse(true);

        when(variantService.setVariantActive(201L, true)).thenReturn(variant);

        ResponseEntity<ProductVariantResponse> response = controller.updateVariantStatus(201L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().active());

        verify(variantService).setVariantActive(201L, true);
    }

    private ProductVariantResponse variantResponse(boolean active) {
        return new ProductVariantResponse(
            201L, 101L, "SAM-S25-8-BLK", "8 GB / Black",
            new BigDecimal("74999.00"), null, "INR",
            active, 162, 0L, List.of());
    }
}
