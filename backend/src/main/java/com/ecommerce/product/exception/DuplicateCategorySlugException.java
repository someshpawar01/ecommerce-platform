package com.ecommerce.product.exception;

public class DuplicateCategorySlugException extends RuntimeException {

    public DuplicateCategorySlugException(String slug) {
        super("Category slug already exists: " + slug);
    }
}
