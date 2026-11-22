package com.ecommerce.product.exception;

public class DuplicateBrandException extends RuntimeException {

    public DuplicateBrandException(String name) {
        super("Brand already exists: " + name);
    }
}
