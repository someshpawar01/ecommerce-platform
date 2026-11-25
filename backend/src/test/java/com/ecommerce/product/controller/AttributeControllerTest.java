package com.ecommerce.product.controller;

import com.ecommerce.product.dto.response.AttributeResponse;
import com.ecommerce.product.dto.response.AttributeValueResponse;
import com.ecommerce.product.service.AttributeService;
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
class AttributeControllerTest {

    @Mock
    private AttributeService attributeService;

    @InjectMocks
    private AttributeController controller;

    @Test
    void shouldReturnAttributes() {
        AttributeResponse color = new AttributeResponse(1L, "color", "Color");

        when(attributeService.getAttributes()).thenReturn(List.of(color));

        ResponseEntity<List<AttributeResponse>> response = controller.getAttributes();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Color", response.getBody().get(0).name());
    }

    @Test
    void shouldReturnEmptyAttributeList() {
        when(attributeService.getAttributes()).thenReturn(List.of());

        ResponseEntity<List<AttributeResponse>> response = controller.getAttributes();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void shouldReturnAttributeById() {
        AttributeResponse color = new AttributeResponse(1L, "color", "Color");

        when(attributeService.getAttribute(1L)).thenReturn(color);

        ResponseEntity<AttributeResponse> response = controller.getAttribute(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Color", response.getBody().name());
    }

    @Test
    void shouldReturnAttributeValues() {
        AttributeValueResponse black = new AttributeValueResponse(10L, 1L, "Black", 0);

        when(attributeService.getAttributeValues(1L)).thenReturn(List.of(black));

        ResponseEntity<List<AttributeValueResponse>> response = controller.getAttributeValues(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Black", response.getBody().get(0).value());
    }
}
