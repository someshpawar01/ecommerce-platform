package com.ecommerce.product.controller;

import com.ecommerce.product.dto.request.CreateProductVariantRequest;
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
class ProductVariantManagementControllerTest {

    @Mock
    private ProductVariantManagementService variantService;

    @InjectMocks
    private ProductVariantManagementController controller;

    @Test
    void shouldCreateVariantAndReturn201() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "SAM-S25-8-BLK", "8 GB / Black", new BigDecimal("74999.00"),
            null, 162, List.of(10L, 20L));

        ProductVariantResponse variant = variantResponse();

        when(variantService.createVariant(101L, request)).thenReturn(variant);

        ResponseEntity<ProductVariantResponse> response = controller.createVariant(101L, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("/api/v1/admin/variants/201", response.getHeaders().getLocation().toString());
        assertEquals(201L, response.getBody().id());

        verify(variantService).createVariant(101L, request);
    }

    @Test
    void shouldReturnProductVariants() {
        ProductVariantResponse variant = variantResponse();

        when(variantService.getProductVariants(101L)).thenReturn(List.of(variant));

        ResponseEntity<List<ProductVariantResponse>> response = controller.getProductVariants(101L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("SAM-S25-8-BLK", response.getBody().getFirst().sku());

        verify(variantService).getProductVariants(101L);
    }

    @Test
    void shouldReturnEmptyVariantList() {
        when(variantService.getProductVariants(101L)).thenReturn(List.of());

        ResponseEntity<List<ProductVariantResponse>> response = controller.getProductVariants(101L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    private ProductVariantResponse variantResponse() {
        return new ProductVariantResponse(
            201L, 101L, "SAM-S25-8-BLK", "8 GB / Black",
            new BigDecimal("74999.00"), null, "INR",
            true, 162, 0L, List.of());
    }
}
