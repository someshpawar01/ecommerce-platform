package com.ecommerce.identity.exception;

public class InvalidVerificationTokenException extends RuntimeException {

  public InvalidVerificationTokenException() {
    super("The verification link is invalid, expired, or already used");
  }
}
