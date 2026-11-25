package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.response.CategoryResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.exception.CategoryNotFoundException;
import com.ecommerce.product.policy.CategoryVisibilityPolicy;
import com.ecommerce.product.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryVisibilityPolicy categoryVisibilityPolicy;

    @InjectMocks
    private CategoryServiceImpl service;

    @Test
    void shouldReturnActiveRootCategories() {
        Category electronics = Category.builder()
            .id(1L)
            .name("Electronics")
            .slug("electronics")
            .active(true)
            .sortOrder(1)
            .build();

        Category fashion = Category.builder()
            .id(2L)
            .name("Fashion")
            .slug("fashion")
            .active(true)
            .sortOrder(2)
            .build();


        when(categoryRepository.findByParentIsNullAndActiveTrueOrderBySortOrderAscNameAscIdAsc())
            .thenReturn(List.of(electronics, fashion));

        List<CategoryResponse> response = service.getRootCategories();

        assertEquals(2, response.size());
        assertEquals("Electronics", response.get(0).name());
        assertEquals("Fashion", response.get(1).name());
    }

    @Test
    void shouldReturnCategoryBySlug() {
        Category electronics = Category.builder()
            .id(1L)
            .name("Electronics")
            .slug("electronics")
            .active(true)
            .build();

        when(categoryVisibilityPolicy.isVisible(electronics)).thenReturn(true);
        when(categoryRepository.findBySlug("electronics")).thenReturn(Optional.of(electronics));

        CategoryResponse response = service.getCategoryBySlug("electronics");

        assertEquals(1L, response.id());
        assertEquals("electronics", response.slug());
    }

    @Test
    void shouldRejectInactiveCategory() {
        Category category = Category.builder()
            .id(1L)
            .name("Electronics")
            .slug("electronics")
            .active(false)
            .build();

        when(categoryVisibilityPolicy.isVisible(category)).thenReturn(false);
        when(categoryRepository.findBySlug("electronics")).thenReturn(Optional.of(category));

        assertThrows(CategoryNotFoundException.class, () -> service.getCategoryBySlug("electronics"));
    }

    @Test
    void shouldRejectCategoryWithInactiveAncestor() {
        Category electronics = Category.builder()
            .id(1L)
            .name("Electronics")
            .active(false)
            .build();

        Category mobiles = Category.builder()
            .id(2L)
            .name("Mobiles")
            .active(true)
            .parent(electronics)
            .build();

        Category smartphones = Category.builder()
            .id(3L)
            .name("Smartphones")
            .slug("smartphones")
            .active(true)
            .parent(mobiles)
            .build();

        when(categoryVisibilityPolicy.isVisible(smartphones)).thenReturn(false);
        when(categoryRepository.findBySlug("smartphones")).thenReturn(Optional.of(smartphones));

        assertThrows(CategoryNotFoundException.class, () -> service.getCategoryBySlug("smartphones"));
    }

    @Test
    void shouldReturnChildCategories() {
        Category electronics = Category.builder()
            .id(1L)
            .name("Electronics")
            .slug("electronics")
            .active(true)
            .build();

        Category mobiles = Category.builder()
            .id(2L)
            .name("Mobiles")
            .slug("mobiles")
            .parent(electronics)
            .active(true)
            .build();

        when(categoryVisibilityPolicy.isVisible(electronics)).thenReturn(true);
        when(categoryRepository.findBySlug("electronics")).thenReturn(Optional.of(electronics));
        when(categoryRepository.findByParentIdAndActiveTrueOrderBySortOrderAscNameAscIdAsc(1L))
            .thenReturn(List.of(mobiles));

        List<CategoryResponse> response = service.getChildCategories("electronics");

        assertEquals(1, response.size());
        assertEquals("Mobiles", response.getFirst().name());
        assertEquals(1L, response.getFirst().parentId());
    }


}
