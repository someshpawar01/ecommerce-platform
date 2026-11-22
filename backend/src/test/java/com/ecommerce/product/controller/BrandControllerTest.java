package com.ecommerce.product.controller;

import com.ecommerce.product.dto.response.BrandResponse;
import com.ecommerce.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandControllerTest {

    @Mock
    private BrandService brandService;

    @InjectMocks
    private BrandController controller;

    @Test
    void shouldReturnActiveBrands() {
        BrandResponse samsung = new BrandResponse(
            1L, "Samsung", "samsung", "Electronics", true);

        BrandResponse apple = new BrandResponse(
            2L, "Apple", "apple", "Technology", true);

        when(brandService.getActiveBrands()).thenReturn(List.of(apple, samsung));

        ResponseEntity<List<BrandResponse>> response = controller.getActiveBrands();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Apple", response.getBody().get(0).name());

        verify(brandService).getActiveBrands();
    }

    @Test
    void shouldReturnEmptyBrandList() {
        when(brandService.getActiveBrands()).thenReturn(List.of());

        ResponseEntity<List<BrandResponse>> response = controller.getActiveBrands();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void shouldReturnBrandBySlug() {
        BrandResponse samsung = new BrandResponse(
            1L, "Samsung", "samsung", "Electronics", true);

        when(brandService.getBrandBySlug("samsung")).thenReturn(samsung);

        ResponseEntity<BrandResponse> response = controller.getBrandBySlug("samsung");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Samsung", response.getBody().name());
        assertEquals("samsung", response.getBody().slug());

        verify(brandService).getBrandBySlug("samsung");
    }
}
