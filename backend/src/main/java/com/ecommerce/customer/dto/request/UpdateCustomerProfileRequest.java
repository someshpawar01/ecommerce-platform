package com.ecommerce.customer.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateCustomerProfileRequest(
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName
) {
}
