package com.ecommerce.product.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlugGeneratorTest {

    private final SlugGenerator slugGenerator = new SlugGenerator();

    @Test
    void shouldGenerateSlugFromSimpleName() {
        assertEquals("samsung", slugGenerator.generate("Samsung"));
        assertEquals("apple", slugGenerator.generate("Apple"));
        assertEquals("mobile-phones", slugGenerator.generate("Mobile Phones"));
    }

    @Test
    void shouldConvertUppercaseCharactersToLowercase() {
        assertEquals("samsung-electronics", slugGenerator.generate("SAMSUNG ELECTRONICS"));
    }

    @Test
    void shouldReplaceSpacesWithHyphens() {
        assertEquals("home-kitchen", slugGenerator.generate("Home Kitchen"));
    }

    @Test
    void shouldReplaceSpecialCharactersWithHyphens() {
        assertEquals("home-kitchen", slugGenerator.generate("Home & Kitchen"));
        assertEquals("men-s-fashion", slugGenerator.generate("Men's Fashion"));
        assertEquals("a-b", slugGenerator.generate("A & B"));
    }

    @Test
    void shouldReplaceMultipleSeparatorsWithSingleHyphen() {
        assertEquals("home-kitchen", slugGenerator.generate("Home && Kitchen"));
        assertEquals("mobile-phones", slugGenerator.generate("Mobile---Phones"));
    }

    @Test
    void shouldRemoveLeadingAndTrailingSeparators() {
        assertEquals("electronics", slugGenerator.generate("---Electronics---"));
        assertEquals("samsung", slugGenerator.generate("!!!Samsung!!!"));
    }

    @Test
    void shouldRemoveLeadingAndTrailingSpaces() {
        assertEquals("samsung", slugGenerator.generate("   Samsung   "));
    }

    @Test
    void shouldNormalizeAccentedCharacters() {
        assertEquals("cafe", slugGenerator.generate("Café"));
        assertEquals("cafe-accessories", slugGenerator.generate("Café Accessories"));
    }

    @Test
    void shouldPreserveDigits() {
        assertEquals("iphone-17", slugGenerator.generate("iPhone 17"));
        assertEquals("4k-television", slugGenerator.generate("4K Television"));
    }

    @Test
    void shouldReturnEmptySlugForUnsupportedCharacters() {
        assertEquals("", slugGenerator.generate("!!!"));
        assertEquals("", slugGenerator.generate("@#$%^&"));
    }

    @Test
    void shouldReturnEmptySlugForBlankString() {
        assertEquals("", slugGenerator.generate(""));
        assertEquals("", slugGenerator.generate("   "));
    }

    @Test
    void shouldNormalizeUnicodeWhitespace() {
        assertEquals("samsung", slugGenerator.generate("\u2003Samsung\u2003"));
    }
}
