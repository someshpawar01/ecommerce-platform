package com.ecommerce.identity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ecommerce.customer.entity.CustomerProfile;
import com.ecommerce.customer.repository.CustomerProfileRepository;
import com.ecommerce.identity.entity.Role;
import com.ecommerce.identity.entity.User;
import com.ecommerce.identity.entity.UserRole;
import com.ecommerce.identity.entity.UserRoleId;
import com.ecommerce.identity.enums.UserStatusEnum;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IdentityRepositoryIntegrationTest {

  @Autowired private UserRepository userRepository;

  @Autowired private RoleRepository roleRepository;

  @Autowired private UserRoleRepository userRoleRepository;

  @Autowired private CustomerProfileRepository customerProfileRepository;

  @Autowired private EntityManager entityManager;

  private String uniqueEmail() {
    return "test-" + UUID.randomUUID() + "@example.com";
  }

  @Test
  void shouldSaveAndRetrieveUser() {

    String email = uniqueEmail();

    User user =
        User.builder()
            .email(email)
            .emailNormalized(email)
            .passwordHash("dummy-hash-for-persistence-test")
            .build();

    User savedUser = userRepository.saveAndFlush(user);

    entityManager.clear();

    User retrievedUser = userRepository.findByEmailNormalized(email).orElseThrow();

    assertThat(retrievedUser.getId()).isEqualTo(savedUser.getId());

    assertThat(retrievedUser.getEmail()).isEqualTo(email);

    assertThat(retrievedUser.getStatus()).isEqualTo(UserStatusEnum.UNVERIFIED);

    assertThat(retrievedUser.getCreatedAt()).isNotNull();

    assertThat(retrievedUser.getUpdatedAt()).isNotNull();
  }

  @Test
  void shouldSaveCustomerProfileWithSharedPrimaryKey() {

    String email = uniqueEmail();

    User user =
        User.builder()
            .email(email)
            .emailNormalized(email)
            .passwordHash("dummy-hash-for-persistence-test")
            .build();

    user = userRepository.saveAndFlush(user);

    CustomerProfile profile =
        CustomerProfile.builder().user(user).firstName("Shreya").lastName("Ray").build();

    customerProfileRepository.saveAndFlush(profile);

    entityManager.clear();

    CustomerProfile savedProfile = customerProfileRepository.findById(user.getId()).orElseThrow();

    assertThat(savedProfile.getUserId()).isEqualTo(user.getId());

    assertThat(savedProfile.getFirstName()).isEqualTo("Shreya");

    assertThat(savedProfile.getUser().getId()).isEqualTo(user.getId());
  }

  @Test
  void shouldSaveUserRoleWithCompositePrimaryKey() {

    String email = uniqueEmail();

    User user =
        User.builder()
            .email(email)
            .emailNormalized(email)
            .passwordHash("dummy-hash-for-persistence-test")
            .build();

    user = userRepository.saveAndFlush(user);

    Role customerRole = roleRepository.findByCode("CUSTOMER").orElseThrow();

    UserRole userRole = UserRole.builder().user(user).role(customerRole).build();

    userRoleRepository.saveAndFlush(userRole);

    entityManager.clear();

    UserRoleId id = new UserRoleId(user.getId(), customerRole.getId());

    UserRole savedUserRole = userRoleRepository.findById(id).orElseThrow();

    assertThat(savedUserRole.getUser().getId()).isEqualTo(user.getId());

    assertThat(savedUserRole.getRole().getCode()).isEqualTo("CUSTOMER");

    assertThat(userRoleRepository.existsByUser_IdAndRole_Code(user.getId(), "CUSTOMER")).isTrue();
  }

  @Test
  void shouldRejectDuplicateNormalizedEmail() {

    String email = uniqueEmail();

    User firstUser =
        User.builder().email(email).emailNormalized(email).passwordHash("hash-1").build();

    userRepository.saveAndFlush(firstUser);

    entityManager.clear();

    User secondUser =
        User.builder()
            .email("Another-" + email)
            .emailNormalized(email)
            .passwordHash("hash-2")
            .build();

    assertThrows(
        DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(secondUser));
  }
}
