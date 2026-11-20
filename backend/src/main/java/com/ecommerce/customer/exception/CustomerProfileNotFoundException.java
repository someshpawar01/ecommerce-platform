package com.ecommerce.customer.exception;

public class CustomerProfileNotFoundException extends RuntimeException {

    public CustomerProfileNotFoundException() {
        super("Customer profile not found");
    }
}
