package com.ecommerce.customer.exception;

public class InvalidCustomerAddressException extends RuntimeException {

    public InvalidCustomerAddressException(String message) {
        super(message);
    }
}
