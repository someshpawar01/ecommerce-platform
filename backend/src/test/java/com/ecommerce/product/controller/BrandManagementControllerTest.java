package com.ecommerce.product.controller;

import com.ecommerce.product.dto.request.CreateBrandRequest;
import com.ecommerce.product.dto.request.UpdateBrandRequest;
import com.ecommerce.product.dto.request.UpdateBrandStatusRequest;
import com.ecommerce.product.dto.response.BrandResponse;
import com.ecommerce.product.service.BrandManagementService;
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
class BrandManagementControllerTest {

    @Mock
    private BrandManagementService brandManagementService;

    @InjectMocks
    private BrandManagementController controller;

    @Test
    void shouldCreateBrandAndReturn201() {
        CreateBrandRequest request = new CreateBrandRequest("Samsung", "Electronics");

        BrandResponse brand = new BrandResponse(
            1L, "Samsung", "samsung", "Electronics", true);

        when(brandManagementService.createBrand(request)).thenReturn(brand);

        ResponseEntity<BrandResponse> response = controller.createBrand(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("/api/v1/brands/samsung", response.getHeaders().getLocation().toString());
        assertNotNull(response.getBody());
        assertEquals("Samsung", response.getBody().name());

        verify(brandManagementService).createBrand(request);
    }

    @Test
    void shouldUpdateBrandAndReturn200() {
        UpdateBrandRequest request = new UpdateBrandRequest("Samsung Electronics", null);

        BrandResponse brand = new BrandResponse(
            1L, "Samsung Electronics", "samsung", "Electronics", true);

        when(brandManagementService.updateBrand(1L, request)).thenReturn(brand);

        ResponseEntity<BrandResponse> response = controller.updateBrand(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Samsung Electronics", response.getBody().name());

        verify(brandManagementService).updateBrand(1L, request);
    }

    @Test
    void shouldDeactivateBrandAndReturn200() {
        UpdateBrandStatusRequest request = new UpdateBrandStatusRequest(false);

        BrandResponse brand = new BrandResponse(
            1L, "Samsung", "samsung", "Electronics", false);

        when(brandManagementService.setBrandActive(1L, false)).thenReturn(brand);

        ResponseEntity<BrandResponse> response = controller.updateBrandStatus(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().active());

        verify(brandManagementService).setBrandActive(1L, false);
    }

    @Test
    void shouldReactivateBrandAndReturn200() {
        UpdateBrandStatusRequest request = new UpdateBrandStatusRequest(true);

        BrandResponse brand = new BrandResponse(
            1L, "Samsung", "samsung", "Electronics", true);

        when(brandManagementService.setBrandActive(1L, true)).thenReturn(brand);

        ResponseEntity<BrandResponse> response = controller.updateBrandStatus(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().active());

        verify(brandManagementService).setBrandActive(1L, true);
    }
}
