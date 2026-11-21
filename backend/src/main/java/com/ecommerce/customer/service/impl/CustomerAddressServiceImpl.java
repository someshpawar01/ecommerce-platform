package com.ecommerce.customer.service.impl;

import com.ecommerce.customer.dto.request.CreateCustomerAddressRequest;
import com.ecommerce.customer.dto.request.UpdateCustomerAddressRequest;
import com.ecommerce.customer.dto.response.CustomerAddressResponse;
import com.ecommerce.customer.entity.CustomerAddress;
import com.ecommerce.customer.entity.CustomerProfile;
import com.ecommerce.customer.exception.CustomerAddressNotFoundException;
import com.ecommerce.customer.exception.CustomerProfileNotFoundException;
import com.ecommerce.customer.exception.InvalidCustomerAddressException;
import com.ecommerce.customer.repository.CustomerAddressRepository;
import com.ecommerce.customer.repository.CustomerProfileRepository;
import com.ecommerce.customer.service.CustomerAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private final CustomerAddressRepository addressRepository;
    private final CustomerProfileRepository customerProfileRepository;

    @Override
    @Transactional
    public CustomerAddressResponse createAddress(Long customerUserId, CreateCustomerAddressRequest request) {

        // Lock the customer profile before checking or changing address defaults.
        CustomerProfile profile = customerProfileRepository.findByUserIdForUpdate(customerUserId)
            .orElseThrow(CustomerProfileNotFoundException::new);

        boolean isFirstAddress = !addressRepository.existsByCustomerProfileUserId(customerUserId);

        CustomerAddress address = CustomerAddress.builder()
            .customerProfile(profile)
            .label(request.label())
            .recipientName(request.recipientName().trim())
            .recipientPhoneE164(request.recipientPhoneE164())
            .line1(request.line1().trim())
            .line2(request.line2())
            .landmark(request.landmark())
            .locality(request.locality())
            .city(request.city().trim())
            .state(request.state().trim())
            .postalCode(request.postalCode().trim())
            .countryCode(request.countryCode())
            .isDefault(isFirstAddress)
            .build();

        address = addressRepository.save(address);

        return toResponse(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> getMyAddresses(Long customerUserId) {
        return addressRepository.findByCustomerProfileUserIdOrderByCreatedAtDesc(customerUserId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerAddressResponse getMyAddress(Long customerUserId, Long addressId) {
        CustomerAddress address = addressRepository.findByIdAndCustomerProfileUserId(addressId, customerUserId)
            .orElseThrow(CustomerAddressNotFoundException::new);

        return toResponse(address);
    }

    @Override
    @Transactional
    public CustomerAddressResponse updateAddress(Long customerUserId, Long addressId, UpdateCustomerAddressRequest request) {
        CustomerAddress address = addressRepository.findByIdAndCustomerProfileUserId(addressId, customerUserId)
            .orElseThrow(CustomerAddressNotFoundException::new);

        if (request.label() != null) {
            address.setLabel(request.label().trim());
        }

        if (request.recipientName() != null) {
            address.setRecipientName(requireNonBlank(request.recipientName(), "Recipient name"));
        }

        if (request.recipientPhoneE164() != null) {
            address.setRecipientPhoneE164(request.recipientPhoneE164());
        }

        if (request.line1() != null) {
            address.setLine1(requireNonBlank(request.line1(), "Address line 1"));
        }

        if (request.line2() != null) {
            address.setLine2(request.line2().trim());
        }

        if (request.landmark() != null) {
            address.setLandmark(request.landmark().trim());
        }

        if (request.locality() != null) {
            address.setLocality(request.locality().trim());
        }

        if (request.city() != null) {
            address.setCity(requireNonBlank(request.city(), "City"));
        }

        if (request.state() != null) {
            address.setState(requireNonBlank(request.state(), "State"));
        }

        if (request.postalCode() != null) {
            address.setPostalCode(requireNonBlank(request.postalCode(), "Postal code"));
        }

        if (request.countryCode() != null) {
            address.setCountryCode(request.countryCode());
        }

        return toResponse(address);
    }

    @Override
    @Transactional
    public CustomerAddressResponse setDefaultAddress(Long customerUserId, Long addressId) {

        customerProfileRepository.findByUserIdForUpdate(customerUserId)
            .orElseThrow(CustomerProfileNotFoundException::new);

        CustomerAddress newDefault = addressRepository.findByIdAndCustomerProfileUserId(addressId, customerUserId)
            .orElseThrow(CustomerAddressNotFoundException::new);

        if (newDefault.isDefault()) {
            return toResponse(newDefault);
        }

        addressRepository.findByCustomerProfileUserIdAndIsDefaultTrue(customerUserId)
            .ifPresent(currentDefault -> {
                currentDefault.setDefault(false);
                addressRepository.flush();
            });

        newDefault.setDefault(true);

        return toResponse(newDefault);
    }

    @Override
    @Transactional
    public void deleteAddress(Long customerUserId, Long addressId) {

        customerProfileRepository.findByUserIdForUpdate(customerUserId)
            .orElseThrow(CustomerProfileNotFoundException::new);

        CustomerAddress address = addressRepository.findByIdAndCustomerProfileUserId(addressId, customerUserId)
            .orElseThrow(CustomerAddressNotFoundException::new);

        if (!address.isDefault()) {
            addressRepository.delete(address);
            return;
        }

        Optional<CustomerAddress> replacement = addressRepository
            .findFirstByCustomerProfileUserIdAndIdNotOrderByIdAsc(customerUserId, addressId);

        addressRepository.delete(address);
        addressRepository.flush();

        replacement.ifPresent(candidate -> candidate.setDefault(true));
    }

    private String requireNonBlank(String value, String fieldName) {
        String normalized = value.trim();

        if (normalized.isBlank()) {
            throw new InvalidCustomerAddressException(fieldName + " cannot be blank");
        }

        return normalized;
    }

    private CustomerAddressResponse toResponse(CustomerAddress address) {
        return new CustomerAddressResponse(
            address.getId(),
            address.getLabel(),
            address.getRecipientName(),
            address.getRecipientPhoneE164(),
            address.getLine1(),
            address.getLine2(),
            address.getLandmark(),
            address.getLocality(),
            address.getCity(),
            address.getState(),
            address.getPostalCode(),
            address.getCountryCode(),
            address.isDefault()
        );
    }
}
