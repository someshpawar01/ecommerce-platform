package com.ecommerce.customer.dto.response;

public record CustomerProfileResponse(
    Long userId,
    String email,
    String firstName,
    String lastName
) {
}
