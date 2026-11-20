package com.ecommerce.customer.service.impl;

import com.ecommerce.customer.api.CustomerProfileOperations;
import com.ecommerce.customer.dto.request.UpdateCustomerProfileRequest;
import com.ecommerce.customer.dto.response.CustomerProfileResponse;
import com.ecommerce.customer.entity.CustomerProfile;
import com.ecommerce.customer.exception.CustomerProfileNotFoundException;
import com.ecommerce.customer.exception.InvalidCustomerProfileException;
import com.ecommerce.customer.repository.CustomerProfileRepository;
import com.ecommerce.customer.service.CustomerProfileService;
import com.ecommerce.identity.api.CustomerIdentityReader;
import com.ecommerce.identity.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileOperations, CustomerProfileService {

  private final CustomerProfileRepository customerProfileRepository;
    private final CustomerIdentityReader customerIdentityReader;
  private final EntityManager entityManager;

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void createProfile(Long userId, String firstName, String lastName) {
    User userReference = entityManager.getReference(User.class, userId);

    CustomerProfile profile =
        CustomerProfile.builder()
            .user(userReference)
            .firstName(firstName)
            .lastName(lastName)
            .build();

    customerProfileRepository.save(profile);
  }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponse getMyProfile(Long userId) {
        CustomerProfile profile = findProfile(userId);
        return toResponse(profile);
    }

    @Override
    @Transactional
    public CustomerProfileResponse updateMyProfile(Long userId, UpdateCustomerProfileRequest request) {
        CustomerProfile profile = findProfile(userId);

        if (request.firstName() != null) {
            String firstName = request.firstName().trim();

            if (firstName.isBlank()) {
                throw new InvalidCustomerProfileException("First name cannot be blank");
            }

            profile.setFirstName(firstName);
        }

        if (request.lastName() != null) {
            String lastName = request.lastName().trim();

            if (lastName.isBlank()) {
                throw new InvalidCustomerProfileException("Last name cannot be blank");
            }

            profile.setLastName(lastName);
        }

        return toResponse(profile);
    }

    private CustomerProfile findProfile(Long userId) {
        return customerProfileRepository.findById(userId)
            .orElseThrow(CustomerProfileNotFoundException::new);
    }

    private CustomerProfileResponse toResponse(CustomerProfile profile) {
        Long userId = profile.getUserId();
        String email = customerIdentityReader.getEmail(userId);
        return new CustomerProfileResponse(
            userId,
            email,
            profile.getFirstName(),
            profile.getLastName()
        );
    }
}
