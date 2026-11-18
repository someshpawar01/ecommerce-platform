package com.ecommerce.customer.service.impl;

import com.ecommerce.customer.api.CustomerProfileOperations;
import com.ecommerce.customer.entity.CustomerProfile;
import com.ecommerce.customer.repository.CustomerProfileRepository;
import com.ecommerce.identity.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileOperations {

  private final CustomerProfileRepository customerProfileRepository;
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
}
