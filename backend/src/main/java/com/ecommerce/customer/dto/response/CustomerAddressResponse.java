package com.ecommerce.customer.dto.response;

public record CustomerAddressResponse(
    Long id,
    String label,
    String recipientName,
    String recipientPhoneE164,
    String line1,
    String line2,
    String landmark,
    String locality,
    String city,
    String state,
    String postalCode,
    String countryCode,
    Boolean isDefault
) {
}
