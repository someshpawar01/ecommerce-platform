package com.ecommerce.product.exception;

public class InvalidProductCategoryException extends RuntimeException {
    public InvalidProductCategoryException(String message) {
        super(message);
    }
}
