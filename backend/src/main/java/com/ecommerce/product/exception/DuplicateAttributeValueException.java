package com.ecommerce.product.exception;

public class DuplicateAttributeValueException extends RuntimeException {

    public DuplicateAttributeValueException(String value) {
        super("Attribute value already exists: " + value);
    }
}
