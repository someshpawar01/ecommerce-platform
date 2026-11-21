package com.ecommerce.customer.exception;

public class CustomerAddressNotFoundException extends RuntimeException {

    public CustomerAddressNotFoundException() {
        super("Customer address not found");
    }
}
