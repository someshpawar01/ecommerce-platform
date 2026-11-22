package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.request.CreateBrandRequest;
import com.ecommerce.product.dto.request.UpdateBrandRequest;
import com.ecommerce.product.dto.response.BrandResponse;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.exception.BrandNotFoundException;
import com.ecommerce.product.exception.DuplicateBrandException;
import com.ecommerce.product.exception.InvalidBrandException;
import com.ecommerce.product.repository.BrandRepository;
import com.ecommerce.product.utils.SlugGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandManagementServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @Spy
    private SlugGenerator slugGenerator = new SlugGenerator();

    @InjectMocks
    private BrandManagementServiceImpl service;

    private Brand samsung;

    @BeforeEach
    void setUp() {
        samsung = Brand.builder()
            .id(1L)
            .name("Samsung")
            .slug("samsung")
            .description("Consumer electronics")
            .active(true)
            .build();
    }

    @Test
    void shouldCreateBrandSuccessfully() {
        CreateBrandRequest request = new CreateBrandRequest("Apple", "Consumer technology");

        when(brandRepository.save(any(Brand.class)))
            .thenAnswer(invocation -> {
                Brand brand = invocation.getArgument(0);
                brand.setId(2L);
                return brand;
            });

        BrandResponse response = service.createBrand(request);

        assertAll(
            () -> assertEquals(2L, response.id()),
            () -> assertEquals("Apple", response.name()),
            () -> assertEquals("apple", response.slug()),
            () -> assertEquals("Consumer technology", response.description()),
            () -> assertTrue(response.active())
        );

        verify(brandRepository).existsByNameIgnoreCase("Apple");
        verify(brandRepository).existsBySlug("apple");
        verify(brandRepository).save(any(Brand.class));
    }

    @Test
    void shouldTrimBrandNameDuringCreation() {
        CreateBrandRequest request = new CreateBrandRequest("   Samsung   ", null);

        when(brandRepository.save(any(Brand.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        BrandResponse response = service.createBrand(request);

        assertEquals("Samsung", response.name());
        assertEquals("samsung", response.slug());

        verify(brandRepository).existsByNameIgnoreCase("Samsung");
    }

    @Test
    void shouldRejectDuplicateBrandName() {
        CreateBrandRequest request = new CreateBrandRequest("Samsung", null);

        when(brandRepository.existsByNameIgnoreCase("Samsung"))
            .thenReturn(true);

        assertThrows(DuplicateBrandException.class, () -> service.createBrand(request));

        verify(brandRepository, never()).save(any(Brand.class));
    }

    @Test
    void shouldRejectBlankBrandName() {
        CreateBrandRequest request = new CreateBrandRequest("   ", null);

        assertThrows(InvalidBrandException.class, () -> service.createBrand(request));

        verifyNoInteractions(brandRepository);
    }

    @Test
    void shouldRejectNullBrandName() {
        CreateBrandRequest request = new CreateBrandRequest(null, null);

        assertThrows(InvalidBrandException.class, () -> service.createBrand(request));

        verifyNoInteractions(brandRepository);
    }

    @Test
    void shouldRejectBrandNameExceedingMaximumLength() {
        String longName = "A".repeat(151);

        CreateBrandRequest request = new CreateBrandRequest(longName, null);

        assertThrows(InvalidBrandException.class, () -> service.createBrand(request));

        verifyNoInteractions(brandRepository);
    }

    @Test
    void shouldRejectNameThatCannotProduceSlug() {
        CreateBrandRequest request = new CreateBrandRequest("!!!", null);

        assertThrows(InvalidBrandException.class, () -> service.createBrand(request));

        verify(brandRepository, never()).save(any(Brand.class));
    }

    @Test
    void shouldGenerateUniqueSlugWhenCollisionExists() {
        CreateBrandRequest request = new CreateBrandRequest("A & B", null);

        when(brandRepository.existsBySlug("a-b")).thenReturn(true);
        when(brandRepository.existsBySlug("a-b-2")).thenReturn(true);
        when(brandRepository.existsBySlug("a-b-3")).thenReturn(false);

        when(brandRepository.save(any(Brand.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        BrandResponse response = service.createBrand(request);

        assertEquals("a-b-3", response.slug());

        verify(brandRepository).existsBySlug("a-b");
        verify(brandRepository).existsBySlug("a-b-2");
        verify(brandRepository).existsBySlug("a-b-3");
    }

    @Test
    void shouldCreateBrandWithNullDescription() {
        CreateBrandRequest request = new CreateBrandRequest("Apple", null);

        when(brandRepository.save(any(Brand.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        BrandResponse response = service.createBrand(request);

        assertNull(response.description());
    }

    @Test
    void shouldNormalizeDescriptionDuringCreation() {
        CreateBrandRequest request = new CreateBrandRequest("Apple", "  Consumer technology  ");

        when(brandRepository.save(any(Brand.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        BrandResponse response = service.createBrand(request);

        assertEquals("Consumer technology", response.description());
    }

    @Test
    void shouldRejectDescriptionExceedingMaximumLength() {
        CreateBrandRequest request = new CreateBrandRequest("Apple", "A".repeat(1001));

        assertThrows(InvalidBrandException.class, () -> service.createBrand(request));

        verify(brandRepository, never()).save(any(Brand.class));
    }

    @Test
    void shouldUpdateBrandNameWithoutChangingSlug() {
        when(brandRepository.findById(1L))
            .thenReturn(Optional.of(samsung));

        UpdateBrandRequest request = new UpdateBrandRequest("Samsung Electronics", null);

        BrandResponse response = service.updateBrand(1L, request);

        assertEquals("Samsung Electronics", response.name());
        assertEquals("samsung", response.slug());

        verify(brandRepository, never()).save(any(Brand.class));
    }

    @Test
    void shouldAllowUpdatingNameWithDifferentCase() {
        when(brandRepository.findById(1L))
            .thenReturn(Optional.of(samsung));

        UpdateBrandRequest request = new UpdateBrandRequest("SAMSUNG", null);

        BrandResponse response = service.updateBrand(1L, request);

        assertEquals("SAMSUNG", response.name());

        verify(brandRepository, never()).existsByNameIgnoreCase(anyString());
    }

    @Test
    void shouldRejectDuplicateNameDuringUpdate() {
        when(brandRepository.findById(1L))
            .thenReturn(Optional.of(samsung));

        when(brandRepository.existsByNameIgnoreCase("Apple"))
            .thenReturn(true);

        UpdateBrandRequest request = new UpdateBrandRequest("Apple", null);

        assertThrows(DuplicateBrandException.class, () -> service.updateBrand(1L, request));

        assertEquals("Samsung", samsung.getName());
    }

    @Test
    void shouldRejectBlankNameDuringUpdate() {
        when(brandRepository.findById(1L))
            .thenReturn(Optional.of(samsung));

        UpdateBrandRequest request = new UpdateBrandRequest("   ", null);

        assertThrows(InvalidBrandException.class, () -> service.updateBrand(1L, request));
    }

    @Test
    void shouldRejectExcessivelyLongNameDuringUpdate() {
        when(brandRepository.findById(1L))
            .thenReturn(Optional.of(samsung));

        UpdateBrandRequest request = new UpdateBrandRequest("A".repeat(151), null);

        assertThrows(InvalidBrandException.class, () -> service.updateBrand(1L, request));
    }

    @Test
    void shouldUpdateDescription() {
        when(brandRepository.findById(1L))
            .thenReturn(Optional.of(samsung));

        UpdateBrandRequest request = new UpdateBrandRequest(null, "Updated description");

        BrandResponse response = service.updateBrand(1L, request);

        assertEquals("Updated description", response.description());
        assertEquals("Samsung", response.name());
    }

    @Test
    void shouldClearDescriptionUsingEmptyString() {
        when(brandRepository.findById(1L))
            .thenReturn(Optional.of(samsung));

        UpdateBrandRequest request = new UpdateBrandRequest(null, "");

        BrandResponse response = service.updateBrand(1L, request);

        assertEquals("", response.description());
    }

    @Test
    void shouldPreserveExistingDescriptionWhenOmitted() {
        when(brandRepository.findById(1L))
            .thenReturn(Optional.of(samsung));

        UpdateBrandRequest request = new UpdateBrandRequest("Samsung Electronics", null);

        BrandResponse response = service.updateBrand(1L, request);

        assertEquals("Consumer electronics", response.description());
    }

    @Test
    void shouldRejectLongDescriptionDuringUpdate() {
        when(brandRepository.findById(1L))
            .thenReturn(Optional.of(samsung));

        UpdateBrandRequest request = new UpdateBrandRequest(null, "A".repeat(1001));

        assertThrows(InvalidBrandException.class, () -> service.updateBrand(1L, request));
    }

    @Test
    void shouldAcceptEmptyUpdateRequest() {
        when(brandRepository.findById(1L))
            .thenReturn(Optional.of(samsung));

        UpdateBrandRequest request = new UpdateBrandRequest(null, null);

        BrandResponse response = service.updateBrand(1L, request);

        assertEquals("Samsung", response.name());
        assertEquals("samsung", response.slug());
        assertEquals("Consumer electronics", response.description());
    }

    @Test
    void shouldRejectUpdatingNonexistentBrand() {
        when(brandRepository.findById(999L))
            .thenReturn(Optional.empty());

        assertThrows(BrandNotFoundException.class,
            () -> service.updateBrand(999L, new UpdateBrandRequest("Apple", null)));
    }

    @Test
    void shouldDeactivateBrand() {
        when(brandRepository.findById(1L))
            .thenReturn(Optional.of(samsung));

        BrandResponse response = service.setBrandActive(1L, false);

        assertFalse(response.active());
        assertFalse(samsung.getActive());
    }

    @Test
    void shouldReactivateBrand() {
        samsung.setActive(false);

        when(brandRepository.findById(1L))
            .thenReturn(Optional.of(samsung));

        BrandResponse response = service.setBrandActive(1L, true);

        assertTrue(response.active());
        assertTrue(samsung.getActive());
    }

    @Test
    void shouldAllowRepeatedActivation() {
        when(brandRepository.findById(1L))
            .thenReturn(Optional.of(samsung));

        BrandResponse response = service.setBrandActive(1L, true);

        assertTrue(response.active());

        verify(brandRepository, never()).save(any(Brand.class));
    }

    @Test
    void shouldRejectStatusUpdateForNonexistentBrand() {
        when(brandRepository.findById(999L))
            .thenReturn(Optional.empty());

        assertThrows(BrandNotFoundException.class, () -> service.setBrandActive(999L, false));
    }
}
