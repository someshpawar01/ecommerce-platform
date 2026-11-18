package com.ecommerce.identity.dto.response;

import com.ecommerce.identity.enums.UserStatusEnum;

public record RegisterCustomerResponse(
    Long userId, String email, UserStatusEnum status, String message) {}
