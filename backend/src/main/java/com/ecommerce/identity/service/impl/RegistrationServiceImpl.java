package com.ecommerce.identity.service.impl;

import com.ecommerce.customer.api.CustomerProfileOperations;
import com.ecommerce.identity.constants.RoleCodes;
import com.ecommerce.identity.constants.UserStatusReasons;
import com.ecommerce.identity.dto.request.RegisterCustomerRequest;
import com.ecommerce.identity.dto.response.RegisterCustomerResponse;
import com.ecommerce.identity.entity.Role;
import com.ecommerce.identity.entity.User;
import com.ecommerce.identity.entity.UserRole;
import com.ecommerce.identity.entity.UserStatusHistory;
import com.ecommerce.identity.enums.UserStatusEnum;
import com.ecommerce.identity.exception.EmailAlreadyExistsException;
import com.ecommerce.identity.exception.IdentityConfigurationException;
import com.ecommerce.identity.exception.InvalidPasswordException;
import com.ecommerce.identity.repository.RoleRepository;
import com.ecommerce.identity.repository.UserRepository;
import com.ecommerce.identity.repository.UserRoleRepository;
import com.ecommerce.identity.repository.UserStatusHistoryRepository;
import com.ecommerce.identity.service.EmailVerificationTokenService;
import com.ecommerce.identity.service.RegistrationService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.ecommerce.identity.utils.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // Lombok generates a constructor for all required final fields.
public class RegistrationServiceImpl implements RegistrationService {

  private final UserRepository userRepository;

  private final RoleRepository roleRepository;

  private final UserRoleRepository userRoleRepository;

  private final CustomerProfileOperations customerProfileOperations;

  private final UserStatusHistoryRepository userStatusHistoryRepository;

  private final PasswordEncoder passwordEncoder;

  private final EmailVerificationTokenService emailVerificationTokenService;

  private final PasswordValidator passwordValidator;

  @Override
  @Transactional
  public RegisterCustomerResponse registerCustomer(RegisterCustomerRequest request) {

    // 1. Normalize email
    String normalizedEmail =
        request.email().trim().toLowerCase(Locale.ROOT); // uses locale-neutral case conversion.

    // 2. Check for an existing account
    if (userRepository.existsByEmailNormalized(normalizedEmail)) {
      throw new EmailAlreadyExistsException();
    }

    // 3. Retrieve CUSTOMER role
    Role customerRole =
        roleRepository
            .findByCode(RoleCodes.CUSTOMER)
            .orElseThrow(
                () ->
                    new IdentityConfigurationException("Required CUSTOMER role is not configured"));

    // 4. Hash password
      passwordValidator.validate(request.password());

    String passwordHash = passwordEncoder.encode(request.password());

    // 5. Create User
    User user =
        User.builder()
            .email(request.email().trim())
            .emailNormalized(normalizedEmail)
            .passwordHash(passwordHash)
            .build();

    user = userRepository.save(user);

    // 6. Create CustomerProfile
    customerProfileOperations.createProfile(
        user.getId(),
        request.firstName().trim(),
        request.lastName() == null ? null : request.lastName().trim());

    // 7. Assign CUSTOMER role
    UserRole userRole = UserRole.builder().user(user).role(customerRole).build();

    userRoleRepository.save(userRole);

    // 8. Record initial account status
    UserStatusHistory statusHistory =
        UserStatusHistory.builder()
            .user(user)
            .newStatus(UserStatusEnum.UNVERIFIED)
            .reason(UserStatusReasons.CUSTOMER_REGISTRATION)
            .build();

    userStatusHistoryRepository.save(statusHistory);

    emailVerificationTokenService.issueVerificationToken(user);

    // 9. Return response
    return new RegisterCustomerResponse(
        user.getId(),
        user.getEmail(),
        user.getStatus(),
        "Registration successful. Email verification is required.");
  }
}
