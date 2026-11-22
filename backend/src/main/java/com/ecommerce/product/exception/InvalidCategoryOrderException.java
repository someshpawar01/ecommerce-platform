package com.ecommerce.product.exception;

public class InvalidCategoryOrderException extends RuntimeException {

    public InvalidCategoryOrderException(String message) {
        super(message);
    }
}
