package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.request.CreateProductVariantRequest;
import com.ecommerce.product.dto.request.UpdateProductVariantRequest;
import com.ecommerce.product.dto.response.ProductVariantResponse;
import com.ecommerce.product.entity.*;
import com.ecommerce.product.entity.id.VariantAttributeValueId;
import com.ecommerce.product.enums.ProductStatusEnum;
import com.ecommerce.product.exception.*;
import com.ecommerce.product.mapper.ProductVariantResponseMapper;
import com.ecommerce.product.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductVariantManagementServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private AttributeValueRepository attributeValueRepository;

    @Mock
    private VariantAttributeValueRepository variantAttributeValueRepository;

    @Mock
    private ProductVariantResponseMapper responseMapper;

    @InjectMocks
    private ProductVariantManagementServiceImpl service;

    private Product product;
    private ProductVariant variant;
    private Attribute color;
    private Attribute ram;
    private AttributeValue black;
    private AttributeValue eightGb;

    @BeforeEach
    void setUp() {
        product = Product.builder()
            .id(101L)
            .name("Samsung Galaxy S25")
            .status(ProductStatusEnum.DRAFT)
            .build();

        variant = ProductVariant.builder()
            .id(201L)
            .product(product)
            .sku("SAM-S25-8-BLK")
            .variantName("8 GB / Black")
            .price(new BigDecimal("74999.00"))
            .compareAtPrice(new BigDecimal("79999.00"))
            .currencyCode("INR")
            .active(true)
            .weightGrams(162)
            .version(0L)
            .build();

        color = Attribute.builder().id(1L).code("color").name("Color").build();
        ram = Attribute.builder().id(2L).code("ram").name("RAM").build();

        black = AttributeValue.builder()
            .id(10L).attribute(color).value("Black").build();

        eightGb = AttributeValue.builder()
            .id(20L).attribute(ram).value("8 GB").build();
    }

    // -------------------------------------------------------------------------
    // CREATE VARIANT
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateVariantWithAttributes() {
        CreateProductVariantRequest request = createRequest(List.of(10L, 20L));

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(attributeValueRepository.findAllById(anySet())).thenReturn(List.of(black, eightGb));
        when(variantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant saved = invocation.getArgument(0);
            saved.setId(201L);
            return saved;
        });

        service.createVariant(101L, request);

        ArgumentCaptor<ProductVariant> variantCaptor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).saveAndFlush(variantCaptor.capture());

        ProductVariant saved = variantCaptor.getValue();

        assertAll(
            () -> assertEquals(product, saved.getProduct()),
            () -> assertEquals("SAM-S25-8-BLK", saved.getSku()),
            () -> assertEquals(new BigDecimal("74999.00"), saved.getPrice()),
            () -> assertEquals(new BigDecimal("79999.00"), saved.getCompareAtPrice()),
            () -> assertEquals("INR", saved.getCurrencyCode()),
            () -> assertTrue(saved.getActive()),
            () -> assertEquals(162, saved.getWeightGrams())
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VariantAttributeValue>> assignmentsCaptor = ArgumentCaptor.forClass(List.class);

        verify(variantAttributeValueRepository).saveAllAndFlush(assignmentsCaptor.capture());

        List<VariantAttributeValue> assignments = assignmentsCaptor.getValue();

        assertEquals(2, assignments.size());

        assertTrue(assignments.stream().anyMatch(a ->
            a.getAttributeValueId().equals(10L)
                && a.getId().equals(new VariantAttributeValueId(201L, 1L))));

        assertTrue(assignments.stream().anyMatch(a ->
            a.getAttributeValueId().equals(20L)
                && a.getId().equals(new VariantAttributeValueId(201L, 2L))));
    }

    @Test
    void shouldCreateVariantWithoutAttributes() {
        CreateProductVariantRequest request = createRequest(List.of());

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(variantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant saved = invocation.getArgument(0);
            saved.setId(201L);
            return saved;
        });

        service.createVariant(101L, request);

        verify(variantAttributeValueRepository, never()).saveAllAndFlush(anyList());
        verifyNoInteractions(attributeValueRepository);
    }

    @Test
    void shouldNormalizeSkuAndVariantName() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "  SAM-S25-8-BLK  ", "  8 GB / Black  ",
            new BigDecimal("74999.00"), null, null, List.of());

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(variantRepository.saveAndFlush(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createVariant(101L, request);

        ArgumentCaptor<ProductVariant> captor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).saveAndFlush(captor.capture());

        assertEquals("SAM-S25-8-BLK", captor.getValue().getSku());
        assertEquals("8 GB / Black", captor.getValue().getVariantName());
    }

    @Test
    void shouldRejectNonexistentProduct() {
        when(productRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
            () -> service.createVariant(999L, createRequest(List.of())));

        verifyNoInteractions(variantRepository, attributeValueRepository);
    }

    @Test
    void shouldRejectCreatingVariantForActiveProduct() {
        product.setStatus(ProductStatusEnum.ACTIVE);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductStateException.class,
            () -> service.createVariant(101L, createRequest(List.of())));

        verifyNoInteractions(variantRepository, attributeValueRepository);
    }

    @Test
    void shouldRejectCreatingVariantForArchivedProduct() {
        product.setStatus(ProductStatusEnum.ARCHIVED);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductStateException.class,
            () -> service.createVariant(101L, createRequest(List.of())));
    }

    @Test
    void shouldRejectDuplicateSku() {
        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(variantRepository.existsBySku("SAM-S25-8-BLK")).thenReturn(true);

        assertThrows(DuplicateProductVariantException.class,
            () -> service.createVariant(101L, createRequest(List.of())));

        verify(variantRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectBlankSku() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "   ", null, BigDecimal.TEN, null, null, List.of());

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductVariantException.class, () -> service.createVariant(101L, request));

        verifyNoInteractions(variantRepository);
    }

    @Test
    void shouldRejectLongSku() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "A".repeat(101), null, BigDecimal.TEN, null, null, List.of());

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductVariantException.class, () -> service.createVariant(101L, request));
    }

    @Test
    void shouldRejectNegativePrice() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "SKU-001", null, new BigDecimal("-1.00"), null, null, List.of());

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductVariantException.class, () -> service.createVariant(101L, request));
    }

    @Test
    void shouldRejectInvalidPriceScale() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "SKU-001", null, new BigDecimal("100.123"), null, null, List.of());

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductVariantException.class, () -> service.createVariant(101L, request));
    }

    @Test
    void shouldRejectCompareAtPriceBelowSellingPrice() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "SKU-001", null, new BigDecimal("1000.00"), new BigDecimal("900.00"), null, List.of());

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductVariantException.class, () -> service.createVariant(101L, request));
    }

    @Test
    void shouldRejectNonpositiveWeight() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "SKU-001", null, BigDecimal.TEN, null, 0, List.of());

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductVariantException.class, () -> service.createVariant(101L, request));
    }

    @Test
    void shouldRejectNullAttributeValueList() {
        CreateProductVariantRequest request = createRequest(null);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductVariantException.class, () -> service.createVariant(101L, request));
    }

    @Test
    void shouldRejectDuplicateAttributeValueIds() {
        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductVariantException.class,
            () -> service.createVariant(101L, createRequest(List.of(10L, 10L))));

        verifyNoInteractions(attributeValueRepository);
    }

    @Test
    void shouldRejectNonexistentAttributeValue() {
        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(attributeValueRepository.findAllById(anySet())).thenReturn(List.of(black));

        assertThrows(InvalidProductVariantException.class,
            () -> service.createVariant(101L, createRequest(List.of(10L, 999L))));
    }

    @Test
    void shouldRejectMultipleValuesForSameAttribute() {
        AttributeValue white = AttributeValue.builder()
            .id(11L).attribute(color).value("White").build();

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(attributeValueRepository.findAllById(anySet())).thenReturn(List.of(black, white));

        assertThrows(InvalidProductVariantException.class,
            () -> service.createVariant(101L, createRequest(List.of(10L, 11L))));
    }

    @Test
    void shouldRejectDuplicateAttributeCombination() {
        VariantAttributeValue assignment = assignment(variant, black);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(attributeValueRepository.findAllById(anySet())).thenReturn(List.of(black));
        when(variantRepository.findByProductIdOrderByIdAsc(101L)).thenReturn(List.of(variant));
        when(variantAttributeValueRepository.findAllByVariantIdsWithDetails(List.of(201L)))
            .thenReturn(List.of(assignment));

        assertThrows(DuplicateProductVariantException.class,
            () -> service.createVariant(101L, createRequest(List.of(10L))));

        verify(variantRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectDuplicateEmptyCombination() {
        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderByIdAsc(101L)).thenReturn(List.of(variant));

        assertThrows(DuplicateProductVariantException.class,
            () -> service.createVariant(101L, createRequest(List.of())));

        verify(variantRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectSameCombinationRegardlessOfValueOrder() {
        VariantAttributeValue blackAssignment = assignment(variant, black);
        VariantAttributeValue ramAssignment = assignment(variant, eightGb);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(attributeValueRepository.findAllById(anySet())).thenReturn(List.of(eightGb, black));
        when(variantRepository.findByProductIdOrderByIdAsc(101L)).thenReturn(List.of(variant));
        when(variantAttributeValueRepository.findAllByVariantIdsWithDetails(List.of(201L)))
            .thenReturn(List.of(blackAssignment, ramAssignment));

        assertThrows(DuplicateProductVariantException.class,
            () -> service.createVariant(101L, createRequest(List.of(20L, 10L))));
    }

    @Test
    void shouldAcquireProductLockBeforeCheckingSku() {
        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(variantRepository.existsBySku("SAM-S25-8-BLK")).thenReturn(true);

        assertThrows(DuplicateProductVariantException.class,
            () -> service.createVariant(101L, createRequest(List.of())));

        var order = inOrder(productRepository, variantRepository);

        order.verify(productRepository).findByIdForUpdate(101L);
        order.verify(variantRepository).existsBySku("SAM-S25-8-BLK");
    }

    // -------------------------------------------------------------------------
    // GET PRODUCT VARIANTS
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnVariantsForProduct() {
        when(productRepository.existsById(101L)).thenReturn(true);
        when(variantRepository.findByProductIdOrderByIdAsc(101L)).thenReturn(List.of(variant));

        List<ProductVariantResponse> responses = service.getProductVariants(101L);

        assertEquals(1, responses.size());
        verify(variantAttributeValueRepository).findAllByVariantIdsWithDetails(List.of(201L));
        verify(responseMapper).toResponse(variant, List.of());
    }

    @Test
    void shouldReturnEmptyListWhenProductHasNoVariants() {
        when(productRepository.existsById(101L)).thenReturn(true);

        assertTrue(service.getProductVariants(101L).isEmpty());

        verifyNoInteractions(variantAttributeValueRepository, responseMapper);
    }

    @Test
    void shouldRejectListingVariantsForNonexistentProduct() {
        assertThrows(ProductNotFoundException.class, () -> service.getProductVariants(999L));

        verifyNoInteractions(variantRepository, variantAttributeValueRepository);
    }

    @Test
    void shouldReturnVariantById() {
        when(variantRepository.findById(201L)).thenReturn(Optional.of(variant));

        service.getVariant(201L);

        verify(variantAttributeValueRepository).findAllByVariantIdsWithDetails(List.of(201L));
        verify(responseMapper).toResponse(variant, List.of());
    }

    @Test
    void shouldRejectNonexistentVariant() {
        when(variantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductVariantNotFoundException.class, () -> service.getVariant(999L));

        verifyNoInteractions(variantAttributeValueRepository, responseMapper);
    }

    // -------------------------------------------------------------------------
    // UPDATE VARIANT
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateVariantSku() {
        UpdateProductVariantRequest request = new UpdateProductVariantRequest(
            "NEW-SKU", null, null, null, null);

        when(variantRepository.findById(201L)).thenReturn(Optional.of(variant));

        service.updateVariant(201L, request);

        assertEquals("NEW-SKU", variant.getSku());
        verify(variantRepository).existsBySkuAndIdNot("NEW-SKU", 201L);
        verify(variantRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateSkuDuringUpdate() {
        UpdateProductVariantRequest request = new UpdateProductVariantRequest(
            "DUPLICATE-SKU", null, null, null, null);

        when(variantRepository.findById(201L)).thenReturn(Optional.of(variant));
        when(variantRepository.existsBySkuAndIdNot("DUPLICATE-SKU", 201L)).thenReturn(true);

        assertThrows(DuplicateProductVariantException.class, () -> service.updateVariant(201L, request));

        assertEquals("SAM-S25-8-BLK", variant.getSku());
    }

    @Test
    void shouldUpdateVariantPrice() {
        UpdateProductVariantRequest request = new UpdateProductVariantRequest(
            null, null, new BigDecimal("75999.00"), null, null);

        when(variantRepository.findById(201L)).thenReturn(Optional.of(variant));

        service.updateVariant(201L, request);

        assertEquals(new BigDecimal("75999.00"), variant.getPrice());
    }

    @Test
    void shouldRejectPriceAboveExistingCompareAtPrice() {
        UpdateProductVariantRequest request = new UpdateProductVariantRequest(
            null, null, new BigDecimal("89999.00"), null, null);

        when(variantRepository.findById(201L)).thenReturn(Optional.of(variant));

        assertThrows(InvalidProductVariantException.class, () -> service.updateVariant(201L, request));

        assertEquals(new BigDecimal("74999.00"), variant.getPrice());
    }

    @Test
    void shouldUpdateVariantNameAndWeight() {
        UpdateProductVariantRequest request = new UpdateProductVariantRequest(
            null, "Updated Variant", null, null, 180);

        when(variantRepository.findById(201L)).thenReturn(Optional.of(variant));

        service.updateVariant(201L, request);

        assertEquals("Updated Variant", variant.getVariantName());
        assertEquals(180, variant.getWeightGrams());
    }

    @Test
    void shouldPreserveFieldsWhenUpdateRequestIsEmpty() {
        UpdateProductVariantRequest request = new UpdateProductVariantRequest(
            null, null, null, null, null);

        when(variantRepository.findById(201L)).thenReturn(Optional.of(variant));

        service.updateVariant(201L, request);

        assertEquals("SAM-S25-8-BLK", variant.getSku());
        assertEquals(new BigDecimal("74999.00"), variant.getPrice());
        assertEquals(162, variant.getWeightGrams());
    }

    @Test
    void shouldRejectUpdatingVariantOfActiveProduct() {
        product.setStatus(ProductStatusEnum.ACTIVE);
        when(variantRepository.findById(201L)).thenReturn(Optional.of(variant));

        assertThrows(InvalidProductStateException.class,
            () -> service.updateVariant(201L, new UpdateProductVariantRequest(
                "UPDATED", null, null, null, null)));

        verifyNoInteractions(variantAttributeValueRepository, responseMapper);
    }

    @Test
    void shouldRejectUpdatingNonexistentVariant() {
        when(variantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductVariantNotFoundException.class,
            () -> service.updateVariant(999L, new UpdateProductVariantRequest(
                "UPDATED", null, null, null, null)));
    }

    // -------------------------------------------------------------------------
    // VARIANT STATUS
    // -------------------------------------------------------------------------

    @Test
    void shouldDeactivateVariant() {
        when(variantRepository.findById(201L)).thenReturn(Optional.of(variant));

        service.setVariantActive(201L, false);

        assertFalse(variant.getActive());
    }

    @Test
    void shouldReactivateVariant() {
        variant.setActive(false);
        when(variantRepository.findById(201L)).thenReturn(Optional.of(variant));

        service.setVariantActive(201L, true);

        assertTrue(variant.getActive());
    }

    @Test
    void shouldAllowRepeatedActivation() {
        when(variantRepository.findById(201L)).thenReturn(Optional.of(variant));

        service.setVariantActive(201L, true);

        assertTrue(variant.getActive());
        verify(variantRepository, never()).save(any());
    }

    @Test
    void shouldRejectStatusChangeForActiveProduct() {
        product.setStatus(ProductStatusEnum.ACTIVE);
        when(variantRepository.findById(201L)).thenReturn(Optional.of(variant));

        assertThrows(InvalidProductStateException.class, () -> service.setVariantActive(201L, false));
    }

    @Test
    void shouldRejectStatusChangeForNonexistentVariant() {
        when(variantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductVariantNotFoundException.class, () -> service.setVariantActive(999L, false));
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private CreateProductVariantRequest createRequest(List<Long> attributeValueIds) {
        return new CreateProductVariantRequest(
            "SAM-S25-8-BLK",
            "8 GB / Black",
            new BigDecimal("74999.00"),
            new BigDecimal("79999.00"),
            162,
            attributeValueIds);
    }

    private VariantAttributeValue assignment(ProductVariant productVariant, AttributeValue value) {
        return VariantAttributeValue.builder()
            .id(new VariantAttributeValueId(productVariant.getId(), value.getAttribute().getId()))
            .variant(productVariant)
            .attribute(value.getAttribute())
            .attributeValueId(value.getId())
            .attributeValue(value)
            .build();
    }
}
