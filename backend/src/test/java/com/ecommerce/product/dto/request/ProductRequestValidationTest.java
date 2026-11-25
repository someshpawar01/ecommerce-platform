package com.ecommerce.product.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void shouldAcceptValidCreateRequest() {
        CreateProductRequest request = new CreateProductRequest("Samsung Galaxy S25", 3L, null, null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectBlankProductName() {
        CreateProductRequest request = new CreateProductRequest("   ", null, null, null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNullProductName() {
        CreateProductRequest request = new CreateProductRequest(null, null, null, null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectLongProductName() {
        CreateProductRequest request = new CreateProductRequest("A".repeat(256), null, null, null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectLongShortDescription() {
        CreateProductRequest request = new CreateProductRequest("Phone", null, "A".repeat(501), null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptPartialUpdate() {
        UpdateProductRequest request = new UpdateProductRequest(null, null, "Updated description", null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptValidCategoryAssignment() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(1L, 2L), 1L);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNullCategoryList() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(null, null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNullCategoryIdInsideList() {
        AssignProductCategoriesRequest request =
            new AssignProductCategoriesRequest(java.util.Arrays.asList(1L, null), 1L);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptEmptyCategoryAssignment() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(), null);

        assertTrue(validator.validate(request).isEmpty());
    }
}
