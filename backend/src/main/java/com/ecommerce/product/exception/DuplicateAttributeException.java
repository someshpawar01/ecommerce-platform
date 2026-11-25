package com.ecommerce.product.exception;

public class DuplicateAttributeException extends RuntimeException {

    public DuplicateAttributeException(String code) {
        super("Attribute code already exists: " + code);
    }
}
