package com.ecommerce.customer.service;

import com.ecommerce.customer.dto.request.UpdateCustomerProfileRequest;
import com.ecommerce.customer.dto.response.CustomerProfileResponse;

public interface CustomerProfileService {



    CustomerProfileResponse getMyProfile(Long userId);

    CustomerProfileResponse updateMyProfile(Long userId, UpdateCustomerProfileRequest request);
}
