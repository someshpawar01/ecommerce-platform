package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.request.CreateCategoryRequest;
import com.ecommerce.product.dto.request.ReorderCategoriesRequest;
import com.ecommerce.product.dto.request.UpdateCategoryRequest;
import com.ecommerce.product.dto.response.CategoryResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.CategoryHierarchyLock;
import com.ecommerce.product.exception.CategoryNotFoundException;
import com.ecommerce.product.exception.InvalidCategoryException;
import com.ecommerce.product.exception.InvalidCategoryHierarchyException;
import com.ecommerce.product.exception.InvalidCategoryOrderException;
import com.ecommerce.product.repository.CategoryHierarchyLockRepository;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.utils.CategorySlugGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryManagementServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryHierarchyLockRepository hierarchyLockRepository;

    @Spy
    private CategorySlugGenerator slugGenerator = new CategorySlugGenerator();

    @InjectMocks
    private CategoryManagementServiceImpl service;

    @Test
    void shouldCreateRootCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest("Home & Kitchen", null, "Home products");

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));
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

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));
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

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));
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
        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));
        assertThrows(InvalidCategoryException.class, () -> service.createCategory(request));

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldRejectNonexistentParent() {
        CreateCategoryRequest request = new CreateCategoryRequest("Tablets", 999L, null);

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));
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

        UpdateCategoryRequest request = new UpdateCategoryRequest("Mobile Phones", null);

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

        UpdateCategoryRequest request = new UpdateCategoryRequest("   ", null);

        assertThrows(InvalidCategoryException.class, () -> service.updateCategory(5L, request));
    }

    @Test
    void shouldUpdateDescription() {
        Category category = Category.builder()
            .id(5L)
            .name("Mobiles")
            .slug("mobiles")
            .description("Old description")
            .sortOrder(1)
            .build();

        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        UpdateCategoryRequest request = new UpdateCategoryRequest(null, "Mobile devices");

        CategoryResponse response = service.updateCategory(5L, request);

        assertEquals("Mobile devices", response.description());
        assertEquals(1, response.sortOrder());
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

    @Test
    void shouldMoveCategoryToAnotherParent() {
        Category electronics = Category.builder().id(1L).name("Electronics").build();
        Category mobiles = Category.builder().id(5L).name("Mobiles").parent(electronics).build();
        Category smartphones = Category.builder().id(6L).name("Smartphones").parent(mobiles).build();

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));
        when(categoryRepository.findById(6L)).thenReturn(Optional.of(smartphones));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));
        when(categoryRepository.findMaxChildSortOrder(1L)).thenReturn(2);

        CategoryResponse response = service.changeParent(6L, 1L);

        assertEquals(1L, response.parentId());
        assertEquals(3, response.sortOrder());
        assertSame(electronics, smartphones.getParent());

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldRejectMovingCategoryUnderDescendant() {
        Category electronics = Category.builder().id(1L).build();
        Category mobiles = Category.builder().id(5L).parent(electronics).build();
        Category smartphones = Category.builder().id(6L).parent(mobiles).build();

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));
        when(categoryRepository.findById(6L)).thenReturn(Optional.of(smartphones));

        assertThrows(InvalidCategoryHierarchyException.class, () -> service.changeParent(1L, 6L));

        assertNull(electronics.getParent());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldRejectCategoryBecomingItsOwnParent() {
        Category electronics = Category.builder().id(1L).build();

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));

        assertThrows(InvalidCategoryHierarchyException.class, () -> service.changeParent(1L, 1L));
    }

    @Test
    void shouldMoveCategoryToRoot() {
        Category electronics = Category.builder().id(1L).build();
        Category mobiles = Category.builder().id(5L).parent(electronics).build();

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(mobiles));
        when(categoryRepository.findMaxRootSortOrder()).thenReturn(4);

        CategoryResponse response = service.changeParent(5L, null);

        assertNull(response.parentId());
        assertNull(mobiles.getParent());
        assertEquals(5, response.sortOrder());
    }

    @Test
    void shouldNotChangeCategoryWhenParentIsUnchanged() {
        Category electronics = Category.builder().id(1L).build();
        Category mobiles = Category.builder().id(5L).parent(electronics).sortOrder(2).build();

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(mobiles));

        CategoryResponse response = service.changeParent(5L, 1L);

        assertEquals(1L, response.parentId());
        assertEquals(2, response.sortOrder());

        verify(categoryRepository, never()).findMaxChildSortOrder(anyLong());
    }

    @Test
    void shouldReorderChildCategories() {
        Category electronics = Category.builder().id(1L).name("Electronics").build();

        Category mobiles = Category.builder()
            .id(5L).name("Mobiles").parent(electronics).sortOrder(1).build();

        Category laptops = Category.builder()
            .id(6L).name("Laptops").parent(electronics).sortOrder(2).build();

        Category tablets = Category.builder()
            .id(7L).name("Tablets").parent(electronics).sortOrder(3).build();

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));

        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.findAllChildrenForOrdering(1L))
            .thenReturn(List.of(mobiles, laptops, tablets));

        ReorderCategoriesRequest request = new ReorderCategoriesRequest(1L, List.of(6L, 7L, 5L));

        List<CategoryResponse> responses = service.reorderCategories(request);

        assertEquals(List.of(6L, 7L, 5L), responses.stream().map(CategoryResponse::id).toList());

        assertEquals(1, laptops.getSortOrder());
        assertEquals(2, tablets.getSortOrder());
        assertEquals(3, mobiles.getSortOrder());

        verify(categoryRepository, never()).saveAll(any());
    }

    @Test
    void shouldRejectDuplicateCategoryIds() {
        Category electronics = Category.builder().id(1L).build();
        Category mobiles = Category.builder().id(5L).parent(electronics).build();
        Category laptops = Category.builder().id(6L).parent(electronics).build();

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));

        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.findAllChildrenForOrdering(1L)).thenReturn(List.of(mobiles, laptops));

        ReorderCategoriesRequest request = new ReorderCategoriesRequest(1L, List.of(5L, 5L));

        assertThrows(InvalidCategoryOrderException.class, () -> service.reorderCategories(request));
    }

    @Test
    void shouldRejectIncompleteSiblingList() {
        Category electronics = Category.builder().id(1L).build();

        Category mobiles = Category.builder().id(5L).parent(electronics).build();
        Category laptops = Category.builder().id(6L).parent(electronics).build();
        Category tablets = Category.builder().id(7L).parent(electronics).build();

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));

        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.findAllChildrenForOrdering(1L))
            .thenReturn(List.of(mobiles, laptops, tablets));

        ReorderCategoriesRequest request = new ReorderCategoriesRequest(1L, List.of(6L, 5L));

        assertThrows(InvalidCategoryOrderException.class, () -> service.reorderCategories(request));
    }

    @Test
    void shouldReorderRootCategories() {
        Category electronics = Category.builder().id(1L).sortOrder(1).build();
        Category fashion = Category.builder().id(2L).sortOrder(2).build();

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));

        when(categoryRepository.findAllRootCategoriesForOrdering())
            .thenReturn(List.of(electronics, fashion));

        ReorderCategoriesRequest request = new ReorderCategoriesRequest(null, List.of(2L, 1L));

        List<CategoryResponse> responses = service.reorderCategories(request);

        assertEquals(2L, responses.get(0).id());
        assertEquals(1, fashion.getSortOrder());
        assertEquals(2, electronics.getSortOrder());
    }

    @Test
    void shouldRejectCategoryFromAnotherParent() {
        Category electronics = Category.builder().id(1L).build();
        Category fashion = Category.builder().id(2L).build();

        Category mobiles = Category.builder().id(5L).parent(electronics).build();
        Category laptops = Category.builder().id(6L).parent(electronics).build();

        Category menswear = Category.builder().id(10L).parent(fashion).build();

        when(hierarchyLockRepository.findByIdForUpdate((byte) 1))
            .thenReturn(Optional.of(new CategoryHierarchyLock()));

        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.findAllChildrenForOrdering(1L)).thenReturn(List.of(mobiles, laptops));

        ReorderCategoriesRequest request = new ReorderCategoriesRequest(1L, List.of(5L, 10L));

        assertThrows(InvalidCategoryOrderException.class, () -> service.reorderCategories(request));
    }



}
