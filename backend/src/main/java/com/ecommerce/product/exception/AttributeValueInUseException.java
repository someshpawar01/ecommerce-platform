package com.ecommerce.product.exception;

public class AttributeValueInUseException extends RuntimeException {

    public AttributeValueInUseException() {
        super("Attribute value is already used by product variants");
    }
}
