package com.ecommerce.product.exception;

public class BrandNotFoundException extends RuntimeException {

    public BrandNotFoundException() {
        super("Brand not found");
    }
}
