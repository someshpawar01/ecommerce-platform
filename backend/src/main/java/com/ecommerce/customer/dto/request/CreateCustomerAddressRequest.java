package com.ecommerce.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerAddressRequest(

    @Size(max = 40)
    String label,

    @NotBlank
    @Size(max = 150)
    String recipientName,

    @NotBlank
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must follow E.164 format")
    String recipientPhoneE164,

    @NotBlank
    @Size(max = 255)
    String line1,

    @Size(max = 255)
    String line2,

    @Size(max = 150)
    String landmark,

    @Size(max = 150)
    String locality,

    @NotBlank
    @Size(max = 100)
    String city,

    @NotBlank
    @Size(max = 100)
    String state,

    @NotBlank
    @Size(max = 20)
    String postalCode,

    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must contain two uppercase letters")
    String countryCode
) {
}
