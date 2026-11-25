package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.response.ProductCategoryResponse;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductCategory;
import com.ecommerce.product.entity.id.ProductCategoryId;
import com.ecommerce.product.enums.ProductStatusEnum;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductResponseMapperTest {

    private final ProductResponseMapper mapper = new ProductResponseMapper();

    @Test
    void shouldMapProductWithBrandAndCategories() {
        Brand brand = Brand.builder()
            .id(1L)
            .name("Samsung")
            .active(true)
            .build();

        Product product = Product.builder()
            .id(101L)
            .name("Samsung Galaxy S25")
            .slug("samsung-galaxy-s25")
            .brand(brand)
            .shortDescription("Flagship smartphone")
            .description("Detailed product description")
            .status(ProductStatusEnum.DRAFT)
            .version(0L)
            .build();

        Category electronics = Category.builder()
            .id(2L)
            .name("Electronics")
            .slug("electronics")
            .active(true)
            .build();

        Category smartphones = Category.builder()
            .id(3L)
            .name("Smartphones")
            .slug("smartphones")
            .active(true)
            .build();

        ProductCategory electronicsAssignment = ProductCategory.builder()
            .id(new ProductCategoryId(101L, 2L))
            .product(product)
            .category(electronics)
            .primary(false)
            .build();

        ProductCategory smartphoneAssignment = ProductCategory.builder()
            .id(new ProductCategoryId(101L, 3L))
            .product(product)
            .category(smartphones)
            .primary(true)
            .build();

        ProductResponse response = mapper.toResponse(
            product, List.of(electronicsAssignment, smartphoneAssignment));

        assertAll(
            () -> assertEquals(101L, response.id()),
            () -> assertEquals("Samsung Galaxy S25", response.name()),
            () -> assertEquals("samsung-galaxy-s25", response.slug()),
            () -> assertEquals(1L, response.brandId()),
            () -> assertEquals("Samsung", response.brandName()),
            () -> assertEquals("Flagship smartphone", response.shortDescription()),
            () -> assertEquals("Detailed product description", response.description()),
            () -> assertEquals(ProductStatusEnum.DRAFT, response.status()),
            () -> assertEquals(0L, response.version()),
            () -> assertEquals(2, response.categories().size())
        );

        ProductCategoryResponse primary = response.categories().stream()
            .filter(ProductCategoryResponse::primary)
            .findFirst()
            .orElseThrow();

        assertEquals(3L, primary.categoryId());
        assertEquals("Smartphones", primary.categoryName());
        assertEquals("smartphones", primary.categorySlug());
    }

    @Test
    void shouldMapProductWithoutBrand() {
        Product product = Product.builder()
            .id(102L)
            .name("Generic USB Cable")
            .slug("generic-usb-cable")
            .status(ProductStatusEnum.DRAFT)
            .build();

        ProductResponse response = mapper.toResponse(product, List.of());

        assertNull(response.brandId());
        assertNull(response.brandName());
        assertTrue(response.categories().isEmpty());
    }

    @Test
    void shouldMapProductWithoutCategories() {
        Product product = Product.builder()
            .id(101L)
            .name("Samsung Galaxy S25")
            .slug("samsung-galaxy-s25")
            .status(ProductStatusEnum.DRAFT)
            .build();

        ProductResponse response = mapper.toResponse(product, List.of());

        assertNotNull(response.categories());
        assertTrue(response.categories().isEmpty());
    }

    @Test
    void shouldMapNonPrimaryCategoryCorrectly() {
        Product product = Product.builder()
            .id(101L)
            .name("Samsung Galaxy S25")
            .status(ProductStatusEnum.DRAFT)
            .build();

        Category electronics = Category.builder()
            .id(1L)
            .name("Electronics")
            .slug("electronics")
            .build();

        ProductCategory assignment = ProductCategory.builder()
            .id(new ProductCategoryId(101L, 1L))
            .product(product)
            .category(electronics)
            .primary(false)
            .build();

        ProductResponse response = mapper.toResponse(product, List.of(assignment));

        assertEquals(1, response.categories().size());
        assertFalse(response.categories().get(0).primary());
    }

    @Test
    void shouldPreserveProductStatusEnum() {
        Product product = Product.builder()
            .id(101L)
            .name("Samsung Galaxy S25")
            .status(ProductStatusEnum.ARCHIVED)
            .build();

        ProductResponse response = mapper.toResponse(product, List.of());

        assertEquals(ProductStatusEnum.ARCHIVED, response.status());
    }

    @Test
    void shouldPreserveCategoryOrderProvidedByRepository() {
        Product product = Product.builder()
            .id(101L)
            .name("Samsung Galaxy S25")
            .status(ProductStatusEnum.DRAFT)
            .build();

        Category mobiles = Category.builder()
            .id(2L)
            .name("Mobiles")
            .slug("mobiles")
            .build();

        Category smartphones = Category.builder()
            .id(3L)
            .name("Smartphones")
            .slug("smartphones")
            .build();

        ProductCategory first = ProductCategory.builder()
            .id(new ProductCategoryId(101L, 2L))
            .product(product)
            .category(mobiles)
            .primary(false)
            .build();

        ProductCategory second = ProductCategory.builder()
            .id(new ProductCategoryId(101L, 3L))
            .product(product)
            .category(smartphones)
            .primary(true)
            .build();

        ProductResponse response = mapper.toResponse(product, List.of(first, second));

        assertEquals("Mobiles", response.categories().get(0).categoryName());
        assertEquals("Smartphones", response.categories().get(1).categoryName());
    }
}
