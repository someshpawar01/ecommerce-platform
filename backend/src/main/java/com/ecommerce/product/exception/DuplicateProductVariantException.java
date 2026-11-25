package com.ecommerce.product.exception;

public class DuplicateProductVariantException extends RuntimeException {

    public DuplicateProductVariantException(String message) {
        super(message);
    }
}
