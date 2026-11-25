package com.ecommerce.product.controller;

import com.ecommerce.product.dto.request.AssignProductCategoriesRequest;
import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.request.UpdateProductRequest;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.service.ProductManagementService;
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
class ProductManagementControllerTest {

    @Mock
    private ProductManagementService productManagementService;

    @InjectMocks
    private ProductManagementController controller;

    @Test
    void shouldCreateDraftProduct() {
        CreateProductRequest request = new CreateProductRequest("Samsung Galaxy S25", 3L, null, null);
        ProductResponse product = productResponse(101L);

        when(productManagementService.createProduct(request)).thenReturn(product);

        ResponseEntity<ProductResponse> response = controller.createProduct(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("/api/v1/admin/products/101", response.getHeaders().getLocation().toString());
        assertEquals(101L, response.getBody().id());

        verify(productManagementService).createProduct(request);
    }

    @Test
    void shouldGetProduct() {
        ProductResponse product = productResponse(101L);

        when(productManagementService.getProduct(101L)).thenReturn(product);

        ResponseEntity<ProductResponse> response = controller.getProduct(101L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(101L, response.getBody().id());

        verify(productManagementService).getProduct(101L);
    }

    @Test
    void shouldUpdateProduct() {
        UpdateProductRequest request = new UpdateProductRequest("Galaxy S25 5G", null, null, null);
        ProductResponse product = productResponse(101L);

        when(productManagementService.updateProduct(101L, request)).thenReturn(product);

        ResponseEntity<ProductResponse> response = controller.updateProduct(101L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(productManagementService).updateProduct(101L, request);
    }

    @Test
    void shouldAssignCategories() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(7L), 7L);
        ProductResponse product = productResponse(101L);

        when(productManagementService.assignCategories(101L, request)).thenReturn(product);

        ResponseEntity<ProductResponse> response = controller.assignCategories(101L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(productManagementService).assignCategories(101L, request);
    }

    private ProductResponse productResponse(Long id) {
        return new ProductResponse(
            id, "Samsung Galaxy S25", "samsung-galaxy-s25",
            3L, "Samsung", null, null,
            com.ecommerce.product.enums.ProductStatusEnum.DRAFT,
            null, null, null, 0L, List.of()
        );
    }
}
