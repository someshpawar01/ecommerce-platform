package com.ecommerce.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ecommerce.common.constants.ErrorCodes;
import com.ecommerce.identity.entity.EmailVerificationToken;
import com.ecommerce.identity.entity.User;
import com.ecommerce.identity.enums.UserStatusEnum;
import com.ecommerce.identity.repository.EmailVerificationTokenRepository;
import com.ecommerce.identity.repository.UserRepository;
import com.ecommerce.identity.service.VerificationEmailSender;
import com.ecommerce.identity.utils.SecureTokenGenerator;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmailVerificationControllerIntegrationTest {

  private static final String VERIFY_EMAIL_URL = "/api/v1/auth/verify-email";

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private EmailVerificationTokenRepository tokenRepository;

  @Autowired private SecureTokenGenerator secureTokenGenerator;

  @Autowired private EntityManager entityManager;

  @MockitoBean private VerificationEmailSender verificationEmailSender;

  @Autowired private JdbcTemplate jdbcTemplate;

  private User createUser() {
    String email = "verify-" + UUID.randomUUID() + "@example.com";

    User user =
        User.builder()
            .email(email)
            .emailNormalized(email)
            .passwordHash("test-password-hash")
            .build();

    return userRepository.saveAndFlush(user);
  }

  private EmailVerificationToken createToken(User user, String rawToken, LocalDateTime expiresAt) {
    EmailVerificationToken token =
        EmailVerificationToken.builder()
            .user(user)
            .tokenHash(secureTokenGenerator.hashToken(rawToken))
            .expiresAt(expiresAt)
            .build();

    return tokenRepository.saveAndFlush(token);
  }

  private String newToken() {
    return secureTokenGenerator.generateToken();
  }

  private LocalDateTime utcNow() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }

  private void verifyToken(String rawToken, int expectedStatus) throws Exception {
    String requestBody =
        """
                {
                    "token": "%s"
                }
                """
            .formatted(rawToken);

    mockMvc
        .perform(
            post(VERIFY_EMAIL_URL).contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().is(expectedStatus));
  }

  private void verifyInvalidToken(String rawToken) throws Exception {
    String requestBody =
        """
            {
                "token": "%s"
            }
            """
            .formatted(rawToken);

    mockMvc
        .perform(
            post(VERIFY_EMAIL_URL).contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCodes.INVALID_VERIFICATION_TOKEN));
  }

  @Test
  void shouldVerifyEmailSuccessfully() throws Exception {
    User user = createUser();
    String rawToken = newToken();

    EmailVerificationToken token = createToken(user, rawToken, utcNow().plusHours(24));

    entityManager.clear();

    verifyToken(rawToken, 200);

    entityManager.flush();
    entityManager.clear();

    User updatedUser = userRepository.findById(user.getId()).orElseThrow();
    EmailVerificationToken updatedToken = tokenRepository.findById(token.getId()).orElseThrow();

    assertThat(updatedUser.getStatus()).isEqualTo(UserStatusEnum.ACTIVE);
    assertThat(updatedUser.getEmailVerifiedAt()).isNotNull();
    assertThat(updatedToken.getConsumedAt()).isNotNull();
    assertThat(updatedToken.getRevokedAt()).isNull();

    Long historyCount =
        entityManager
            .createQuery(
                """
                        SELECT COUNT(h) FROM UserStatusHistory h
                        WHERE h.user.id = :userId AND h.newStatus = :status
                        """,
                Long.class)
            .setParameter("userId", user.getId())
            .setParameter("status", UserStatusEnum.ACTIVE)
            .getSingleResult();

    assertThat(historyCount).isEqualTo(1L);
  }

  @Test
  void shouldRejectInvalidToken() throws Exception {
    verifyInvalidToken(newToken());
  }

  @Test
  void shouldRejectExpiredToken() throws Exception {
    User user = createUser();
    String rawToken = newToken();

    EmailVerificationToken token = createToken(user, rawToken, utcNow().plusHours(24));

    LocalDateTime now = utcNow();

    jdbcTemplate.update(
        """
            UPDATE email_verification_tokens
            SET created_at = ?, expires_at = ?
            WHERE id = ?
            """,
        now.minusDays(2),
        now.minusDays(1),
        token.getId());

    entityManager.clear();

    verifyInvalidToken(rawToken);

    entityManager.clear();

    EmailVerificationToken expiredToken = tokenRepository.findById(token.getId()).orElseThrow();

    assertThat(expiredToken.getConsumedAt()).isNull();
    assertThat(userRepository.findById(user.getId()).orElseThrow().getStatus())
        .isEqualTo(UserStatusEnum.UNVERIFIED);
  }

  @Test
  void shouldRejectAlreadyConsumedToken() throws Exception {
    User user = createUser();
    String rawToken = newToken();

    EmailVerificationToken token = createToken(user, rawToken, utcNow().plusHours(24));
    token.setConsumedAt(utcNow());

    tokenRepository.flush();
    entityManager.clear();

    verifyInvalidToken(rawToken);
  }

  @Test
  void shouldRejectRevokedToken() throws Exception {
    User user = createUser();
    String rawToken = newToken();

    EmailVerificationToken token = createToken(user, rawToken, utcNow().plusHours(24));
    token.setRevokedAt(utcNow());

    tokenRepository.flush();
    entityManager.clear();

    verifyInvalidToken(rawToken);
  }

  @Test
  void shouldRejectVerificationForSuspendedAccount() throws Exception {
    User user = createUser();
    user.setStatus(UserStatusEnum.SUSPENDED);

    userRepository.saveAndFlush(user);

    String rawToken = newToken();
    createToken(user, rawToken, utcNow().plusHours(24));

    entityManager.clear();

    verifyToken(rawToken, 409);

    assertThat(userRepository.findById(user.getId()).orElseThrow().getStatus())
        .isEqualTo(UserStatusEnum.SUSPENDED);
  }
}
