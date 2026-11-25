package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.response.AttributeResponse;
import com.ecommerce.product.dto.response.AttributeValueResponse;
import com.ecommerce.product.entity.Attribute;
import com.ecommerce.product.entity.AttributeValue;
import com.ecommerce.product.exception.AttributeNotFoundException;
import com.ecommerce.product.repository.AttributeRepository;
import com.ecommerce.product.repository.AttributeValueRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttributeServiceImplTest {

    @Mock
    private AttributeRepository attributeRepository;

    @Mock
    private AttributeValueRepository attributeValueRepository;

    @InjectMocks
    private AttributeServiceImpl service;

    @Test
    void shouldReturnAttributes() {
        Attribute color = Attribute.builder().id(1L).code("color").name("Color").build();
        Attribute size = Attribute.builder().id(2L).code("size").name("Size").build();

        when(attributeRepository.findAllByOrderByNameAscIdAsc()).thenReturn(List.of(color, size));

        List<AttributeResponse> response = service.getAttributes();

        assertEquals(2, response.size());
        assertEquals("Color", response.get(0).name());
        assertEquals("Size", response.get(1).name());
    }

    @Test
    void shouldReturnEmptyAttributeList() {
        when(attributeRepository.findAllByOrderByNameAscIdAsc()).thenReturn(List.of());

        assertTrue(service.getAttributes().isEmpty());
    }

    @Test
    void shouldReturnAttributeById() {
        Attribute color = Attribute.builder().id(1L).code("color").name("Color").build();

        when(attributeRepository.findById(1L)).thenReturn(Optional.of(color));

        AttributeResponse response = service.getAttribute(1L);

        assertEquals(1L, response.id());
        assertEquals("color", response.code());
        assertEquals("Color", response.name());
    }

    @Test
    void shouldRejectNonexistentAttribute() {
        when(attributeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AttributeNotFoundException.class, () -> service.getAttribute(999L));
    }

    @Test
    void shouldReturnAttributeValues() {
        Attribute color = Attribute.builder().id(1L).code("color").name("Color").build();

        AttributeValue black = AttributeValue.builder()
            .id(10L).attribute(color).value("Black").displayOrder(1).build();

        AttributeValue white = AttributeValue.builder()
            .id(11L).attribute(color).value("White").displayOrder(2).build();

        when(attributeRepository.existsById(1L)).thenReturn(true);
        when(attributeValueRepository.findByAttributeIdOrderByDisplayOrderAscValueAscIdAsc(1L))
            .thenReturn(List.of(black, white));

        List<AttributeValueResponse> response = service.getAttributeValues(1L);

        assertEquals(2, response.size());
        assertEquals("Black", response.get(0).value());
        assertEquals("White", response.get(1).value());
        assertEquals(1L, response.get(0).attributeId());
    }

    @Test
    void shouldReturnEmptyValuesForExistingAttribute() {
        when(attributeRepository.existsById(1L)).thenReturn(true);
        when(attributeValueRepository.findByAttributeIdOrderByDisplayOrderAscValueAscIdAsc(1L))
            .thenReturn(List.of());

        assertTrue(service.getAttributeValues(1L).isEmpty());
    }

    @Test
    void shouldRejectValuesLookupForNonexistentAttribute() {
        when(attributeRepository.existsById(999L)).thenReturn(false);

        assertThrows(AttributeNotFoundException.class, () -> service.getAttributeValues(999L));

        verifyNoInteractions(attributeValueRepository);
    }
}
