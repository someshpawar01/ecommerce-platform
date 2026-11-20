package com.ecommerce.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.identity.entity.EmailVerificationToken;
import com.ecommerce.identity.entity.User;
import com.ecommerce.identity.enums.UserStatusEnum;
import com.ecommerce.identity.repository.EmailVerificationTokenRepository;
import com.ecommerce.identity.repository.UserRepository;
import com.ecommerce.identity.security.AccountRateLimitService;
import com.ecommerce.identity.service.VerificationEmailSender;
import com.ecommerce.identity.utils.SecureTokenGenerator;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResendVerificationControllerIntegrationTest {

  private static final String RESEND_URL = "/api/v1/auth/resend-verification";
  private static final String VERIFY_URL = "/api/v1/auth/verify-email";

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private EmailVerificationTokenRepository tokenRepository;

  @Autowired private SecureTokenGenerator secureTokenGenerator;

  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private VerificationEmailSender verificationEmailSender;

    @MockitoBean
    private AccountRateLimitService accountRateLimitService;

  private LocalDateTime utcNow() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }

  private User createUser(UserStatusEnum status) {
    String email = "resend-" + UUID.randomUUID() + "@example.com";

    User user =
        User.builder()
            .email(email)
            .emailNormalized(email)
            .passwordHash("test-password-hash")
            .status(status)
            .build();

    return userRepository.saveAndFlush(user);
  }

  private EmailVerificationToken createToken(User user) {
    String rawToken = secureTokenGenerator.generateToken();
    return createToken(user, rawToken);
  }

  private EmailVerificationToken createToken(User user, String rawToken) {
    EmailVerificationToken token =
        EmailVerificationToken.builder()
            .user(user)
            .tokenHash(secureTokenGenerator.hashToken(rawToken))
            .expiresAt(utcNow().plusHours(24))
            .build();

    return tokenRepository.saveAndFlush(token);
  }

  private void backdateToken(Long tokenId, LocalDateTime createdAt) {
    jdbcTemplate.update(
        """
            UPDATE email_verification_tokens
            SET created_at = ?
            WHERE id = ?
            """,
        createdAt,
        tokenId);
  }

  private void resendVerification(String email) throws Exception {
    String requestBody =
        """
            {
                "email": "%s"
            }
            """
            .formatted(email);

    mockMvc
        .perform(post(RESEND_URL).contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isAccepted());
  }

  private void verifyEmail(String rawToken, int expectedStatus) throws Exception {
    String requestBody =
        """
            {
                "token": "%s"
            }
            """
            .formatted(rawToken);

    mockMvc
        .perform(post(VERIFY_URL).contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().is(expectedStatus));
  }

  private int countTokens(Long userId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM email_verification_tokens
            WHERE user_id = ?
            """,
            Integer.class,
            userId);

    return count == null ? 0 : count;
  }

  private EmailVerificationToken reloadToken(Long tokenId) {
    return tokenRepository.findById(tokenId).orElseThrow();
  }

  private void deleteTestUser(Long userId) {
    if (userId == null) {
      return;
    }

    jdbcTemplate.update("DELETE FROM email_verification_tokens WHERE user_id = ?", userId);
    jdbcTemplate.update("DELETE FROM user_status_history WHERE user_id = ?", userId);
    jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
    jdbcTemplate.update("DELETE FROM customer_profiles WHERE user_id = ?", userId);
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
  }

  @Test
  void shouldReturnAcceptedForUnknownEmailWithoutSendingEmail() throws Exception {
    String email = "unknown-" + UUID.randomUUID() + "@example.com";

    resendVerification(email);

    verifyNoInteractions(verificationEmailSender);
  }

  @Test
  void shouldNotResendVerificationForActiveAccount() throws Exception {
    User user = createUser(UserStatusEnum.ACTIVE);

    try {
      resendVerification(user.getEmail());

      assertThat(countTokens(user.getId())).isZero();
      verifyNoInteractions(verificationEmailSender);
    } finally {
      deleteTestUser(user.getId());
    }
  }

  @Test
  void shouldNotResendVerificationForSuspendedAccount() throws Exception {
    User user = createUser(UserStatusEnum.SUSPENDED);

    try {
      resendVerification(user.getEmail());

      assertThat(countTokens(user.getId())).isZero();
      verifyNoInteractions(verificationEmailSender);
    } finally {
      deleteTestUser(user.getId());
    }
  }

  @Test
  void shouldNotResendVerificationWithinCooldown() throws Exception {
    User user = createUser(UserStatusEnum.UNVERIFIED);

    try {
      EmailVerificationToken originalToken = createToken(user);

      resendVerification(user.getEmail());

      assertThat(countTokens(user.getId())).isEqualTo(1);
      assertThat(reloadToken(originalToken.getId()).getRevokedAt()).isNull();

      verifyNoInteractions(verificationEmailSender);
    } finally {
      deleteTestUser(user.getId());
    }
  }

  @Test
  void shouldNotResendVerificationWhenDailyLimitIsReached() throws Exception {
    User user = createUser(UserStatusEnum.UNVERIFIED);

    try {
      LocalDateTime now = utcNow();

      for (int i = 0; i < 5; i++) {
        EmailVerificationToken token = createToken(user);
        backdateToken(token.getId(), now.minusHours(i + 1L));
      }

      assertThat(countTokens(user.getId())).isEqualTo(5);

      resendVerification(user.getEmail());

      assertThat(countTokens(user.getId())).isEqualTo(5);
      verifyNoInteractions(verificationEmailSender);
    } finally {
      deleteTestUser(user.getId());
    }
  }

  @Test
  void shouldRevokeOldTokenAndIssueNewVerificationEmail() throws Exception {
    User user = createUser(UserStatusEnum.UNVERIFIED);
    String oldRawToken = secureTokenGenerator.generateToken();

    try {
      EmailVerificationToken oldToken = createToken(user, oldRawToken);

      backdateToken(oldToken.getId(), utcNow().minusMinutes(2));

      resendVerification(user.getEmail());

      assertThat(countTokens(user.getId())).isEqualTo(2);

      EmailVerificationToken revokedToken = reloadToken(oldToken.getId());

      assertThat(revokedToken.getRevokedAt()).isNotNull();
      assertThat(revokedToken.getConsumedAt()).isNull();

      var linkCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

      verify(verificationEmailSender, times(1))
          .sendVerificationEmail(eq(user.getEmail()), linkCaptor.capture());

      String verificationLink = linkCaptor.getValue();

      assertThat(verificationLink).contains("/verify-email?token=");

      String newRawToken = verificationLink.substring(verificationLink.indexOf("token=") + 6);

      assertThat(newRawToken).isNotBlank();
      assertThat(newRawToken).isNotEqualTo(oldRawToken);

      verifyEmail(oldRawToken, 400);

      verifyEmail(newRawToken, 200);

      User verifiedUser = userRepository.findById(user.getId()).orElseThrow();

      assertThat(verifiedUser.getStatus()).isEqualTo(UserStatusEnum.ACTIVE);
      assertThat(verifiedUser.getEmailVerifiedAt()).isNotNull();

      int consumedCount =
          jdbcTemplate.queryForObject(
              """
                SELECT COUNT(*)
                FROM email_verification_tokens
                WHERE user_id = ? AND consumed_at IS NOT NULL
                """,
              Integer.class,
              user.getId());

      assertThat(consumedCount).isEqualTo(1);

      Integer activationCount =
          jdbcTemplate.queryForObject(
              """
                SELECT COUNT(*)
                FROM user_status_history
                WHERE user_id = ? AND new_status = 'ACTIVE'
                """,
              Integer.class,
              user.getId());

      assertThat(activationCount).isEqualTo(1);

    } finally {
      deleteTestUser(user.getId());
    }
  }
}
