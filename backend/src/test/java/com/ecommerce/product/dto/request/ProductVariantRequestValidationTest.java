package com.ecommerce.product.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductVariantRequestValidationTest {

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
        CreateProductVariantRequest request = validRequest();

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptVariantWithoutAttributes() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "USB-001", null, new BigDecimal("299.00"), null, null, List.of());

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNullSku() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            null, null, new BigDecimal("100.00"), null, null, List.of());

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectBlankSku() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "   ", null, new BigDecimal("100.00"), null, null, List.of());

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectLongSku() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "A".repeat(101), null, new BigDecimal("100.00"), null, null, List.of());

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNullPrice() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "SKU-001", null, null, null, null, List.of());

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNegativePrice() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "SKU-001", null, new BigDecimal("-1.00"), null, null, List.of());

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectPriceWithMoreThanTwoDecimalPlaces() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "SKU-001", null, new BigDecimal("100.123"), null, null, List.of());

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectPriceExceedingDatabasePrecision() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "SKU-001", null, new BigDecimal("12345678901.00"), null, null, List.of());

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptZeroPrice() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "FREE-001", null, BigDecimal.ZERO, null, null, List.of());

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNegativeCompareAtPrice() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "SKU-001", null, new BigDecimal("100.00"), new BigDecimal("-1.00"), null, List.of());

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectInvalidWeight() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "SKU-001", null, new BigDecimal("100.00"), null, 0, List.of());

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNullAttributeValueList() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "SKU-001", null, new BigDecimal("100.00"), null, null, null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNullAttributeValueId() {
        CreateProductVariantRequest request = new CreateProductVariantRequest(
            "SKU-001", null, new BigDecimal("100.00"), null, null, Arrays.asList(1L, null));

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptPartialUpdate() {
        UpdateProductVariantRequest request = new UpdateProductVariantRequest(
            null, null, new BigDecimal("500.00"), null, null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectInvalidUpdateSku() {
        UpdateProductVariantRequest request = new UpdateProductVariantRequest(
            "A".repeat(101), null, null, null, null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNegativeUpdatePrice() {
        UpdateProductVariantRequest request = new UpdateProductVariantRequest(
            null, null, new BigDecimal("-100.00"), null, null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptValidStatus() {
        assertTrue(validator.validate(new UpdateProductVariantStatusRequest(true)).isEmpty());
        assertTrue(validator.validate(new UpdateProductVariantStatusRequest(false)).isEmpty());
    }

    @Test
    void shouldRejectNullStatus() {
        assertFalse(validator.validate(new UpdateProductVariantStatusRequest(null)).isEmpty());
    }

    private CreateProductVariantRequest validRequest() {
        return new CreateProductVariantRequest(
            "SAM-S25-8-128-BLK",
            "8 GB / 128 GB / Black",
            new BigDecimal("74999.00"),
            new BigDecimal("79999.00"),
            162,
            List.of(10L, 15L, 20L));
    }
}
