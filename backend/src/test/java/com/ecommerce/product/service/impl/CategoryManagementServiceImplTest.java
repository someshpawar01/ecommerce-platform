package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.request.CreateCategoryRequest;
import com.ecommerce.product.dto.request.UpdateCategoryRequest;
import com.ecommerce.product.dto.response.CategoryResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.exception.CategoryNotFoundException;
import com.ecommerce.product.exception.InvalidCategoryException;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.utils.CategorySlugGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryManagementServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private CategorySlugGenerator slugGenerator = new CategorySlugGenerator();

    @InjectMocks
    private CategoryManagementServiceImpl service;

    @Test
    void shouldCreateRootCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest("Home & Kitchen", null, "Home products");

        when(categoryRepository.existsBySlug("home-kitchen")).thenReturn(false);
        when(categoryRepository.findMaxRootSortOrder()).thenReturn(4);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(10L);
            return category;
        });

        CategoryResponse response = service.createCategory(request);

        assertEquals(10L, response.id());
        assertEquals("home-kitchen", response.slug());
        assertEquals(5, response.sortOrder());
        assertNull(response.parentId());

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void shouldCreateChildCategory() {
        Category electronics = Category.builder()
            .id(1L)
            .name("Electronics")
            .slug("electronics")
            .build();

        CreateCategoryRequest request = new CreateCategoryRequest("Tablets", 1L, "Tablet devices");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));
        when(categoryRepository.existsBySlug("tablets")).thenReturn(false);
        when(categoryRepository.findMaxChildSortOrder(1L)).thenReturn(3);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = service.createCategory(request);

        assertEquals("tablets", response.slug());
        assertEquals(1L, response.parentId());
        assertEquals(4, response.sortOrder());
    }

    @Test
    void shouldGenerateUniqueSlugWhenCollisionExists() {
        CreateCategoryRequest request = new CreateCategoryRequest("Accessories", null, null);

        when(categoryRepository.existsBySlug("accessories")).thenReturn(true);
        when(categoryRepository.existsBySlug("accessories-2")).thenReturn(true);
        when(categoryRepository.existsBySlug("accessories-3")).thenReturn(false);
        when(categoryRepository.findMaxRootSortOrder()).thenReturn(4);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = service.createCategory(request);

        assertEquals("accessories-3", response.slug());
    }

    @Test
    void shouldRejectCategoryNameThatCannotProduceSlug() {
        CreateCategoryRequest request = new CreateCategoryRequest("!!!", null, null);

        assertThrows(InvalidCategoryException.class, () -> service.createCategory(request));

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldRejectNonexistentParent() {
        CreateCategoryRequest request = new CreateCategoryRequest("Tablets", 999L, null);

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> service.createCategory(request));

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldUpdateCategoryNameWithoutChangingSlug() {
        Category category = Category.builder()
            .id(5L)
            .name("Mobiles")
            .slug("mobiles")
            .active(true)
            .build();

        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        UpdateCategoryRequest request = new UpdateCategoryRequest("Mobile Phones", null, null);

        CategoryResponse response = service.updateCategory(5L, request);

        assertEquals("Mobile Phones", response.name());
        assertEquals("mobiles", response.slug());

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldRejectBlankCategoryName() {
        Category category = Category.builder()
            .id(5L)
            .name("Mobiles")
            .slug("mobiles")
            .build();

        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        UpdateCategoryRequest request = new UpdateCategoryRequest("   ", null, null);

        assertThrows(InvalidCategoryException.class, () -> service.updateCategory(5L, request));
    }

    @Test
    void shouldUpdateDescriptionAndSortOrder() {
        Category category = Category.builder()
            .id(5L)
            .name("Mobiles")
            .slug("mobiles")
            .description("Old description")
            .sortOrder(1)
            .build();

        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        UpdateCategoryRequest request = new UpdateCategoryRequest(null, "Mobile devices", 3);

        CategoryResponse response = service.updateCategory(5L, request);

        assertEquals("Mobile devices", response.description());
        assertEquals(3, response.sortOrder());
    }

    @Test
    void shouldDeactivateCategory() {
        Category category = Category.builder()
            .id(1L)
            .name("Electronics")
            .active(true)
            .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse response = service.setCategoryActive(1L, false);

        assertFalse(response.active());
        assertFalse(category.getActive());
    }

    @Test
    void shouldReactivateCategory() {
        Category category = Category.builder()
            .id(1L)
            .name("Electronics")
            .active(false)
            .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse response = service.setCategoryActive(1L, true);

        assertTrue(response.active());
    }

    @Test
    void shouldNotModifyCategoryWhenStatusAlreadyMatches() {
        Category category = Category.builder()
            .id(1L)
            .active(true)
            .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse response = service.setCategoryActive(1L, true);

        assertTrue(response.active());

        verify(categoryRepository, never()).save(any(Category.class));
    }

}
