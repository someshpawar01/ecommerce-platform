package com.ecommerce.customer.api;

public interface CustomerProfileOperations {

  void createProfile(Long userId, String firstName, String lastName);
}
