package com.ecommerce.identity.dto.response;

import java.util.List;

public record CurrentUserResponse(
    Long id,
    String email,
    List<String> roles,
    List<String> permissions
) {
}
