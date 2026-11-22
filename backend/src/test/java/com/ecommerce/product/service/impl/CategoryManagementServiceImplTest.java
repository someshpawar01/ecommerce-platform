package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.request.CreateCategoryRequest;
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


}
