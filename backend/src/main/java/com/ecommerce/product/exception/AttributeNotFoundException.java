package com.ecommerce.product.exception;

public class AttributeNotFoundException extends RuntimeException {

    public AttributeNotFoundException() {
        super("Attribute not found");
    }
}
