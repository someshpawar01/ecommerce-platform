package com.ecommerce.identity.service;

import com.ecommerce.identity.dto.request.RegisterCustomerRequest;
import com.ecommerce.identity.dto.response.RegisterCustomerResponse;

public interface RegistrationService {

  RegisterCustomerResponse registerCustomer(RegisterCustomerRequest request);
}
