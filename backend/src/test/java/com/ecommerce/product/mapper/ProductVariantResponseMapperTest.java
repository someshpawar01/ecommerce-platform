package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.response.ProductVariantResponse;
import com.ecommerce.product.entity.*;
import java.math.BigDecimal;
import java.util.List;

import com.ecommerce.product.entity.id.VariantAttributeValueId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductVariantResponseMapperTest {

    private final ProductVariantResponseMapper mapper = new ProductVariantResponseMapper();

    @Test
    void shouldMapVariantWithoutAttributes() {
        Product product = Product.builder().id(101L).build();

        ProductVariant variant = ProductVariant.builder()
            .id(201L)
            .product(product)
            .sku("USB-001")
            .price(new BigDecimal("299.00"))
            .currencyCode("INR")
            .active(true)
            .version(0L)
            .build();

        ProductVariantResponse response = mapper.toResponse(variant, List.of());

        assertAll(
            () -> assertEquals(201L, response.id()),
            () -> assertEquals(101L, response.productId()),
            () -> assertEquals("USB-001", response.sku()),
            () -> assertEquals(new BigDecimal("299.00"), response.price()),
            () -> assertEquals("INR", response.currencyCode()),
            () -> assertTrue(response.active()),
            () -> assertTrue(response.attributes().isEmpty())
        );
    }

    @Test
    void shouldMapVariantWithAttributes() {
        Product product = Product.builder().id(101L).build();

        ProductVariant variant = ProductVariant.builder()
            .id(201L)
            .product(product)
            .sku("SAM-S25-8-128-BLK")
            .price(new BigDecimal("74999.00"))
            .compareAtPrice(new BigDecimal("79999.00"))
            .currencyCode("INR")
            .active(true)
            .weightGrams(162)
            .build();

        Attribute color = Attribute.builder().id(1L).code("color").name("Color").build();

        AttributeValue black = AttributeValue.builder()
            .id(10L).attribute(color).value("Black").build();

        VariantAttributeValue assignment = VariantAttributeValue.builder()
            .id(new VariantAttributeValueId(201L, 1L))
            .variant(variant)
            .attribute(color)
            .attributeValue(black)
            .build();

        ProductVariantResponse response = mapper.toResponse(variant, List.of(assignment));

        assertEquals(1, response.attributes().size());
        assertEquals("Color", response.attributes().get(0).attributeName());
        assertEquals("color", response.attributes().get(0).attributeCode());
        assertEquals("Black", response.attributes().get(0).value());
        assertEquals(10L, response.attributes().get(0).attributeValueId());
        assertEquals(new BigDecimal("79999.00"), response.compareAtPrice());
        assertEquals(162, response.weightGrams());
    }

    @Test
    void shouldPreserveAttributeOrder() {
        ProductVariant variant = ProductVariant.builder()
            .id(201L)
            .product(Product.builder().id(101L).build())
            .sku("SKU-001")
            .price(BigDecimal.TEN)
            .currencyCode("INR")
            .active(true)
            .build();

        Attribute ram = Attribute.builder().id(1L).code("ram").name("RAM").build();
        Attribute storage = Attribute.builder().id(2L).code("storage").name("Storage").build();

        AttributeValue eightGb = AttributeValue.builder()
            .id(10L).attribute(ram).value("8 GB").build();

        AttributeValue storage128 = AttributeValue.builder()
            .id(20L).attribute(storage).value("128 GB").build();

        VariantAttributeValue first = VariantAttributeValue.builder()
            .variant(variant).attribute(ram).attributeValue(eightGb).build();

        VariantAttributeValue second = VariantAttributeValue.builder()
            .variant(variant).attribute(storage).attributeValue(storage128).build();

        ProductVariantResponse response = mapper.toResponse(variant, List.of(first, second));

        assertEquals("RAM", response.attributes().get(0).attributeName());
        assertEquals("Storage", response.attributes().get(1).attributeName());
    }
}
