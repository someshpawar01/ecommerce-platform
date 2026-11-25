package com.ecommerce.product.policy;

import com.ecommerce.product.entity.Category;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryVisibilityPolicyImplTest {

    private final CategoryVisibilityPolicy policy = new CategoryVisibilityPolicyImpl();

    @Test
    void shouldReturnFalseForNullCategory() {
        assertFalse(policy.isVisible(null));
    }

    @Test
    void shouldReturnTrueForActiveRootCategory() {
        Category electronics = Category.builder()
            .id(1L)
            .active(true)
            .build();

        assertTrue(policy.isVisible(electronics));
    }

    @Test
    void shouldReturnFalseForInactiveRootCategory() {
        Category electronics = Category.builder()
            .id(1L)
            .active(false)
            .build();

        assertFalse(policy.isVisible(electronics));
    }

    @Test
    void shouldReturnFalseForNullActivationStatus() {
        Category electronics = Category.builder()
            .id(1L)
            .active(null)
            .build();

        assertFalse(policy.isVisible(electronics));
    }

    @Test
    void shouldReturnTrueWhenCategoryAndAllAncestorsAreActive() {
        Category electronics = Category.builder()
            .id(1L)
            .active(true)
            .build();

        Category mobiles = Category.builder()
            .id(2L)
            .parent(electronics)
            .active(true)
            .build();

        Category smartphones = Category.builder()
            .id(3L)
            .parent(mobiles)
            .active(true)
            .build();

        assertTrue(policy.isVisible(smartphones));
    }

    @Test
    void shouldReturnFalseWhenDirectParentIsInactive() {
        Category electronics = Category.builder()
            .id(1L)
            .active(false)
            .build();

        Category mobiles = Category.builder()
            .id(2L)
            .parent(electronics)
            .active(true)
            .build();

        assertFalse(policy.isVisible(mobiles));
    }

    @Test
    void shouldReturnFalseWhenDistantAncestorIsInactive() {
        Category electronics = Category.builder()
            .id(1L)
            .active(false)
            .build();

        Category mobiles = Category.builder()
            .id(2L)
            .parent(electronics)
            .active(true)
            .build();

        Category smartphones = Category.builder()
            .id(3L)
            .parent(mobiles)
            .active(true)
            .build();

        assertFalse(policy.isVisible(smartphones));
    }

    @Test
    void shouldReturnFalseWhenCategoryItselfIsInactive() {
        Category electronics = Category.builder()
            .id(1L)
            .active(true)
            .build();

        Category mobiles = Category.builder()
            .id(2L)
            .parent(electronics)
            .active(false)
            .build();

        assertFalse(policy.isVisible(mobiles));
    }

    @Test
    void shouldReturnFalseWhenAncestorHasNullActivationStatus() {
        Category electronics = Category.builder()
            .id(1L)
            .active(null)
            .build();

        Category mobiles = Category.builder()
            .id(2L)
            .parent(electronics)
            .active(true)
            .build();

        assertFalse(policy.isVisible(mobiles));
    }

    @Test
    void shouldReturnFalseWhenHierarchyContainsCycle() {
        Category categoryA = Category.builder()
            .id(1L)
            .active(true)
            .build();

        Category categoryB = Category.builder()
            .id(2L)
            .active(true)
            .build();

        Category categoryC = Category.builder()
            .id(3L)
            .active(true)
            .build();

        categoryA.setParent(categoryB);
        categoryB.setParent(categoryC);
        categoryC.setParent(categoryA);

        assertFalse(policy.isVisible(categoryA));
    }

    @Test
    void shouldReturnFalseForSelfReferencingCategory() {
        Category electronics = Category.builder()
            .id(1L)
            .active(true)
            .build();

        electronics.setParent(electronics);

        assertFalse(policy.isVisible(electronics));
    }

    @Test
    void shouldReturnTrueForDeepValidHierarchy() {
        Category root = Category.builder()
            .id(1L)
            .active(true)
            .build();

        Category level2 = Category.builder()
            .id(2L)
            .parent(root)
            .active(true)
            .build();

        Category level3 = Category.builder()
            .id(3L)
            .parent(level2)
            .active(true)
            .build();

        Category level4 = Category.builder()
            .id(4L)
            .parent(level3)
            .active(true)
            .build();

        Category level5 = Category.builder()
            .id(5L)
            .parent(level4)
            .active(true)
            .build();

        assertTrue(policy.isVisible(level5));
    }
}
