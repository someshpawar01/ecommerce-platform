package com.ecommerce.product.exception;

public class AttributeValueNotFoundException extends RuntimeException {

    public AttributeValueNotFoundException() {
        super("Attribute value not found");
    }
}
