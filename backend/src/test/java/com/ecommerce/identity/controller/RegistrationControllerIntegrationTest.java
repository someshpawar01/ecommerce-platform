package com.ecommerce.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.identity.enums.UserStatusEnum;
import com.ecommerce.identity.repository.UserRepository;
import com.ecommerce.identity.repository.UserRoleRepository;
import com.ecommerce.identity.repository.UserStatusHistoryRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private UserRoleRepository userRoleRepository;

  @Autowired private UserStatusHistoryRepository userStatusHistoryRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private String uniqueEmail() {
    return "test-" + UUID.randomUUID() + "@example.com";
  }

  @Test
  void shouldRegisterCustomerSuccessfully() throws Exception {
    String email = uniqueEmail();
    String password = "MySecurePassword123!";

    String requestBody =
        """
            {
                "firstName": "Shreya",
                "lastName": "Ray",
                "email": "%s",
                "password": "%s"
            }
            """
            .formatted(email, password);

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.status").value("UNVERIFIED"))
        .andExpect(jsonPath("$.userId").isNumber());

    var user = userRepository.findByEmailNormalized(email).orElseThrow();

    assertThat(user.getStatus()).isEqualTo(UserStatusEnum.UNVERIFIED);
    assertThat(user.getPasswordHash()).isNotEqualTo(password);
    assertThat(passwordEncoder.matches(password, user.getPasswordHash())).isTrue();

    assertThat(userRoleRepository.existsByUser_IdAndRole_Code(user.getId(), "CUSTOMER")).isTrue();

    var history = userStatusHistoryRepository.findByUser_IdOrderByChangedAtDesc(user.getId());

    assertThat(history).hasSize(1);
    assertThat(history.getFirst().getNewStatus()).isEqualTo(UserStatusEnum.UNVERIFIED);
  }

  @Test
  void shouldRejectDuplicateEmail() throws Exception {
    String email = uniqueEmail();

    String requestBody =
        """
            {
                "firstName": "Shreya",
                "lastName": "Ray",
                "email": "%s",
                "password": "MySecurePassword123!"
            }
            """
            .formatted(email);

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
  }

  @Test
  void shouldRejectInvalidEmail() throws Exception {
    String requestBody =
        """
            {
                "firstName": "Shreya",
                "lastName": "Ray",
                "email": "invalid-email",
                "password": "MySecurePassword123!"
            }
            """;

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.email").exists());
  }

  @Test
  void shouldRejectBlankFirstName() throws Exception {
    String requestBody =
        """
            {
                "firstName": "",
                "lastName": "Ray",
                "email": "%s",
                "password": "MySecurePassword123!"
            }
            """
            .formatted(uniqueEmail());

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.firstName").exists());
  }

  @Test
  void shouldRejectShortPassword() throws Exception {
    String requestBody =
        """
            {
                "firstName": "Shreya",
                "lastName": "Ray",
                "email": "%s",
                "password": "123"
            }
            """
            .formatted(uniqueEmail());

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.password").exists());
  }

  @Test
  void shouldRejectPasswordExceeding72Bytes() throws Exception {
    String password = "😀".repeat(20);

    String requestBody =
        """
            {
                "firstName": "Shreya",
                "lastName": "Ray",
                "email": "%s",
                "password": "%s"
            }
            """
            .formatted(uniqueEmail(), password);

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
  }
}
