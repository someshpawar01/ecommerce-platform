package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.request.CreateAttributeRequest;
import com.ecommerce.product.dto.request.CreateAttributeValueRequest;
import com.ecommerce.product.dto.request.UpdateAttributeRequest;
import com.ecommerce.product.dto.request.UpdateAttributeValueRequest;
import com.ecommerce.product.dto.response.AttributeResponse;
import com.ecommerce.product.dto.response.AttributeValueResponse;
import com.ecommerce.product.entity.Attribute;
import com.ecommerce.product.entity.AttributeValue;
import com.ecommerce.product.exception.AttributeNotFoundException;
import com.ecommerce.product.exception.AttributeValueInUseException;
import com.ecommerce.product.exception.AttributeValueNotFoundException;
import com.ecommerce.product.exception.DuplicateAttributeException;
import com.ecommerce.product.exception.DuplicateAttributeValueException;
import com.ecommerce.product.exception.InvalidAttributeException;
import com.ecommerce.product.repository.AttributeRepository;
import com.ecommerce.product.repository.AttributeValueRepository;
import com.ecommerce.product.utils.SlugGenerator;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttributeManagementServiceImplTest {

    @Mock
    private AttributeRepository attributeRepository;

    @Mock
    private AttributeValueRepository attributeValueRepository;

    @Spy
    private SlugGenerator slugGenerator = new SlugGenerator();

    @InjectMocks
    private AttributeManagementServiceImpl service;

    private Attribute color;
    private AttributeValue black;

    @BeforeEach
    void setUp() {
        color = Attribute.builder().id(1L).code("color").name("Color").build();

        black = AttributeValue.builder()
            .id(10L)
            .attribute(color)
            .value("Black")
            .displayOrder(0)
            .build();
    }

    @Test
    void shouldCreateAttribute() {
        when(attributeRepository.save(any(Attribute.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AttributeResponse response = service.createAttribute(new CreateAttributeRequest("Color"));

        assertEquals("Color", response.name());
        assertEquals("color", response.code());
        verify(attributeRepository).save(any(Attribute.class));
    }

    @Test
    void shouldNormalizeAttributeName() {
        when(attributeRepository.save(any(Attribute.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AttributeResponse response = service.createAttribute(new CreateAttributeRequest("  Screen Size  "));

        assertEquals("Screen Size", response.name());
        assertEquals("screen-size", response.code());
    }

    @Test
    void shouldRejectDuplicateAttributeCode() {
        when(attributeRepository.existsByCode("color")).thenReturn(true);

        assertThrows(DuplicateAttributeException.class,
            () -> service.createAttribute(new CreateAttributeRequest("Color")));

        verify(attributeRepository, never()).save(any());
    }

    @Test
    void shouldRejectBlankAttributeName() {
        assertThrows(InvalidAttributeException.class,
            () -> service.createAttribute(new CreateAttributeRequest("   ")));

        verifyNoInteractions(attributeRepository);
    }

    @Test
    void shouldRejectNullAttributeName() {
        assertThrows(InvalidAttributeException.class,
            () -> service.createAttribute(new CreateAttributeRequest(null)));
    }

    @Test
    void shouldRejectLongAttributeName() {
        assertThrows(InvalidAttributeException.class,
            () -> service.createAttribute(new CreateAttributeRequest("A".repeat(101))));
    }

    @Test
    void shouldRejectAttributeNameWithoutValidCode() {
        assertThrows(InvalidAttributeException.class,
            () -> service.createAttribute(new CreateAttributeRequest("!!!")));
    }

    @Test
    void shouldUpdateAttributeNameWithoutChangingCode() {
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(color));

        AttributeResponse response = service.updateAttribute(1L, new UpdateAttributeRequest("Product Color"));

        assertEquals("Product Color", response.name());
        assertEquals("color", response.code());

        verify(attributeRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdatingNonexistentAttribute() {
        when(attributeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AttributeNotFoundException.class,
            () -> service.updateAttribute(999L, new UpdateAttributeRequest("Size")));
    }

    @Test
    void shouldRejectBlankAttributeNameDuringUpdate() {
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(color));

        assertThrows(InvalidAttributeException.class,
            () -> service.updateAttribute(1L, new UpdateAttributeRequest("   ")));
    }

    @Test
    void shouldCreateAttributeValue() {
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(color));
        when(attributeValueRepository.save(any(AttributeValue.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AttributeValueResponse response = service.createAttributeValue(
            1L, new CreateAttributeValueRequest("White"));

        assertEquals("White", response.value());
        assertEquals(1L, response.attributeId());
        assertEquals(0, response.displayOrder());
    }

    @Test
    void shouldNormalizeAttributeValue() {
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(color));
        when(attributeValueRepository.save(any(AttributeValue.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AttributeValueResponse response = service.createAttributeValue(
            1L, new CreateAttributeValueRequest("  Dark Blue  "));

        assertEquals("Dark Blue", response.value());
    }

    @Test
    void shouldRejectDuplicateAttributeValue() {
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(color));
        when(attributeValueRepository.existsByAttributeIdAndValueIgnoreCase(1L, "Black"))
            .thenReturn(true);

        assertThrows(DuplicateAttributeValueException.class,
            () -> service.createAttributeValue(1L, new CreateAttributeValueRequest("Black")));

        verify(attributeValueRepository, never()).save(any());
    }

    @Test
    void shouldRejectAttributeValueForNonexistentAttribute() {
        when(attributeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AttributeNotFoundException.class,
            () -> service.createAttributeValue(999L, new CreateAttributeValueRequest("Black")));
    }

    @Test
    void shouldRejectBlankAttributeValue() {
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(color));

        assertThrows(InvalidAttributeException.class,
            () -> service.createAttributeValue(1L, new CreateAttributeValueRequest("  ")));
    }

    @Test
    void shouldRejectLongAttributeValue() {
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(color));

        assertThrows(InvalidAttributeException.class,
            () -> service.createAttributeValue(1L, new CreateAttributeValueRequest("A".repeat(151))));
    }

    @Test
    void shouldUpdateUnusedAttributeValue() {
        when(attributeValueRepository.findById(10L)).thenReturn(Optional.of(black));

        AttributeValueResponse response = service.updateAttributeValue(
            10L, new UpdateAttributeValueRequest("Midnight Black"));

        assertEquals("Midnight Black", response.value());
        assertEquals("Midnight Black", black.getValue());

        verify(attributeValueRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdatingValueUsedByVariant() {
        when(attributeValueRepository.findById(10L)).thenReturn(Optional.of(black));
        when(attributeValueRepository.countVariantReferences(10L)).thenReturn(2L);

        assertThrows(AttributeValueInUseException.class,
            () -> service.updateAttributeValue(10L, new UpdateAttributeValueRequest("White")));

        assertEquals("Black", black.getValue());
    }

    @Test
    void shouldRejectDuplicateValueDuringUpdate() {
        when(attributeValueRepository.findById(10L)).thenReturn(Optional.of(black));
        when(attributeValueRepository.existsByAttributeIdAndValueIgnoreCaseAndIdNot(1L, "White", 10L))
            .thenReturn(true);

        assertThrows(DuplicateAttributeValueException.class,
            () -> service.updateAttributeValue(10L, new UpdateAttributeValueRequest("White")));
    }

    @Test
    void shouldAllowNoOpValueUpdate() {
        when(attributeValueRepository.findById(10L)).thenReturn(Optional.of(black));

        AttributeValueResponse response = service.updateAttributeValue(
            10L, new UpdateAttributeValueRequest("Black"));

        assertEquals("Black", response.value());

        verify(attributeValueRepository, never()).countVariantReferences(anyLong());
        verify(attributeValueRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdatingNonexistentValue() {
        when(attributeValueRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AttributeValueNotFoundException.class,
            () -> service.updateAttributeValue(999L, new UpdateAttributeValueRequest("White")));
    }

    @Test
    void shouldRejectBlankValueDuringUpdate() {
        when(attributeValueRepository.findById(10L)).thenReturn(Optional.of(black));

        assertThrows(InvalidAttributeException.class,
            () -> service.updateAttributeValue(10L, new UpdateAttributeValueRequest("  ")));
    }
}
