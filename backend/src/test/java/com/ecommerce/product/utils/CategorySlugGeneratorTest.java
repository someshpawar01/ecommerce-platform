package com.ecommerce.product.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CategorySlugGeneratorTest {

    private final CategorySlugGenerator generator = new CategorySlugGenerator();

    @Test
    void shouldGenerateSlugFromCategoryName() {
        assertEquals("mobile-phones", generator.generate("Mobile Phones"));
        assertEquals("home-kitchen", generator.generate("Home & Kitchen"));
        assertEquals("men-s-fashion", generator.generate("Men's Fashion"));
        assertEquals("cafe-accessories", generator.generate("Café Accessories"));
    }

    @Test
    void shouldNormalizeRepeatedSeparators() {
        assertEquals("home-kitchen", generator.generate("  Home && Kitchen  "));
        assertEquals("mobile-phones", generator.generate("Mobile---Phones"));
    }

    @Test
    void shouldReturnEmptySlugWhenNoSupportedCharactersExist() {
        assertEquals("", generator.generate("!!!"));
    }
}
