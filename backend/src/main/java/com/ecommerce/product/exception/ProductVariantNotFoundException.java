package com.ecommerce.product.exception;

public class ProductVariantNotFoundException extends RuntimeException {

    public ProductVariantNotFoundException() {
        super("Product variant not found");
    }
}
