package com.ecommerce.customer.service;

import com.ecommerce.customer.dto.request.CreateCustomerAddressRequest;
import com.ecommerce.customer.dto.request.UpdateCustomerAddressRequest;
import com.ecommerce.customer.dto.response.CustomerAddressResponse;

import java.util.List;

public interface CustomerAddressService {

    CustomerAddressResponse createAddress(Long customerUserId, CreateCustomerAddressRequest request);

    List<CustomerAddressResponse> getMyAddresses(Long customerUserId);

    CustomerAddressResponse getMyAddress(Long customerUserId, Long addressId);

    CustomerAddressResponse updateAddress(Long customerUserId, Long addressId, UpdateCustomerAddressRequest request);

    CustomerAddressResponse setDefaultAddress(Long customerUserId, Long addressId);

    void deleteAddress(Long customerUserId, Long addressId);
}
