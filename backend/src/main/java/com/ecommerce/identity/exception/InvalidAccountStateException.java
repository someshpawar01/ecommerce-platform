package com.ecommerce.identity.exception;

public class InvalidAccountStateException extends RuntimeException {

  public InvalidAccountStateException(String message) {
    super(message);
  }
}
