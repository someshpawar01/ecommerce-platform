package com.ecommerce.product.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttributeRequestValidationTest {

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
    void shouldAcceptValidCreateAttributeRequest() {
        assertTrue(validator.validate(new CreateAttributeRequest("Color")).isEmpty());
    }

    @Test
    void shouldRejectNullAttributeName() {
        assertFalse(validator.validate(new CreateAttributeRequest(null)).isEmpty());
    }

    @Test
    void shouldRejectBlankAttributeName() {
        assertFalse(validator.validate(new CreateAttributeRequest("   ")).isEmpty());
    }

    @Test
    void shouldRejectLongAttributeName() {
        assertFalse(validator.validate(new CreateAttributeRequest("A".repeat(101))).isEmpty());
    }

    @Test
    void shouldAcceptValidUpdateAttributeRequest() {
        assertTrue(validator.validate(new UpdateAttributeRequest("Product Color")).isEmpty());
    }

    @Test
    void shouldRejectBlankUpdateAttributeName() {
        assertFalse(validator.validate(new UpdateAttributeRequest("   ")).isEmpty());
    }

    @Test
    void shouldAcceptValidCreateAttributeValueRequest() {
        assertTrue(validator.validate(new CreateAttributeValueRequest("Black")).isEmpty());
    }

    @Test
    void shouldRejectBlankAttributeValue() {
        assertFalse(validator.validate(new CreateAttributeValueRequest("   ")).isEmpty());
    }

    @Test
    void shouldRejectLongAttributeValue() {
        assertFalse(validator.validate(new CreateAttributeValueRequest("A".repeat(151))).isEmpty());
    }

    @Test
    void shouldAcceptValidUpdateAttributeValueRequest() {
        assertTrue(validator.validate(new UpdateAttributeValueRequest("Midnight Black")).isEmpty());
    }

    @Test
    void shouldRejectBlankUpdatedAttributeValue() {
        assertFalse(validator.validate(new UpdateAttributeValueRequest("   ")).isEmpty());
    }

    @Test
    void shouldRejectLongUpdatedAttributeValue() {
        assertFalse(validator.validate(new UpdateAttributeValueRequest("A".repeat(151))).isEmpty());
    }
}
