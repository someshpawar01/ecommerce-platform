package com.ecommerce.product.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrandRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void shouldAcceptValidCreateBrandRequest() {
        CreateBrandRequest request = new CreateBrandRequest("Samsung", "Electronics");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNullBrandNameDuringCreation() {
        CreateBrandRequest request = new CreateBrandRequest(null, null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectBlankBrandNameDuringCreation() {
        CreateBrandRequest request = new CreateBrandRequest("   ", null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectLongBrandNameDuringCreation() {
        CreateBrandRequest request = new CreateBrandRequest("A".repeat(151), null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectLongDescriptionDuringCreation() {
        CreateBrandRequest request = new CreateBrandRequest("Samsung", "A".repeat(1001));

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptValidUpdateBrandRequest() {
        UpdateBrandRequest request = new UpdateBrandRequest("Samsung Electronics", "Updated description");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptPartialUpdateBrandRequest() {
        UpdateBrandRequest request = new UpdateBrandRequest(null, "Updated description");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptEmptyUpdateBrandRequest() {
        UpdateBrandRequest request = new UpdateBrandRequest(null, null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectLongNameDuringUpdate() {
        UpdateBrandRequest request = new UpdateBrandRequest("A".repeat(151), null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectLongDescriptionDuringUpdate() {
        UpdateBrandRequest request = new UpdateBrandRequest(null, "A".repeat(1001));

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptValidBrandStatus() {
        assertTrue(validator.validate(new UpdateBrandStatusRequest(true)).isEmpty());
        assertTrue(validator.validate(new UpdateBrandStatusRequest(false)).isEmpty());
    }

    @Test
    void shouldRejectMissingBrandStatus() {
        UpdateBrandStatusRequest request = new UpdateBrandStatusRequest(null);

        assertFalse(validator.validate(request).isEmpty());
    }
}
