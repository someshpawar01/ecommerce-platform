package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.response.BrandResponse;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.exception.BrandNotFoundException;
import com.ecommerce.product.repository.BrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandServiceImpl service;

    private Brand samsung;
    private Brand apple;
    private Brand sony;

    @BeforeEach
    void setUp() {
        samsung = Brand.builder()
            .id(1L)
            .name("Samsung")
            .slug("samsung")
            .description("Consumer electronics")
            .active(true)
            .build();

        apple = Brand.builder()
            .id(2L)
            .name("Apple")
            .slug("apple")
            .description("Consumer technology")
            .active(true)
            .build();

        sony = Brand.builder()
            .id(3L)
            .name("Sony")
            .slug("sony")
            .description("Entertainment and electronics")
            .active(false)
            .build();
    }

    @Test
    void shouldReturnActiveBrands() {
        when(brandRepository.findByActiveTrueOrderByNameAscIdAsc())
            .thenReturn(List.of(apple, samsung));

        List<BrandResponse> responses = service.getActiveBrands();

        assertEquals(2, responses.size());
        assertEquals("Apple", responses.get(0).name());
        assertEquals("Samsung", responses.get(1).name());
        assertTrue(responses.stream().allMatch(BrandResponse::active));

        verify(brandRepository).findByActiveTrueOrderByNameAscIdAsc();
    }

    @Test
    void shouldReturnEmptyListWhenNoActiveBrandsExist() {
        when(brandRepository.findByActiveTrueOrderByNameAscIdAsc())
            .thenReturn(List.of());

        List<BrandResponse> responses = service.getActiveBrands();

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void shouldReturnActiveBrandBySlug() {
        when(brandRepository.findBySlug("samsung"))
            .thenReturn(Optional.of(samsung));

        BrandResponse response = service.getBrandBySlug("samsung");

        assertEquals(1L, response.id());
        assertEquals("Samsung", response.name());
        assertEquals("samsung", response.slug());
        assertEquals("Consumer electronics", response.description());
        assertTrue(response.active());
    }

    @Test
    void shouldRejectInactiveBrandBySlug() {
        when(brandRepository.findBySlug("sony"))
            .thenReturn(Optional.of(sony));

        assertThrows(BrandNotFoundException.class, () -> service.getBrandBySlug("sony"));
    }

    @Test
    void shouldRejectNonexistentBrandBySlug() {
        when(brandRepository.findBySlug("unknown"))
            .thenReturn(Optional.empty());

        assertThrows(BrandNotFoundException.class, () -> service.getBrandBySlug("unknown"));
    }

    @Test
    void shouldMapBrandResponseCorrectly() {
        when(brandRepository.findBySlug("apple"))
            .thenReturn(Optional.of(apple));

        BrandResponse response = service.getBrandBySlug("apple");

        assertAll(
            () -> assertEquals(2L, response.id()),
            () -> assertEquals("Apple", response.name()),
            () -> assertEquals("apple", response.slug()),
            () -> assertEquals("Consumer technology", response.description()),
            () -> assertTrue(response.active())
        );
    }
}
