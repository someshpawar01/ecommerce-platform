package com.ecommerce.identity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.identity.entity.EmailVerificationToken;
import com.ecommerce.identity.entity.User;
import com.ecommerce.identity.enums.UserStatusEnum;
import com.ecommerce.identity.exception.InvalidVerificationTokenException;
import com.ecommerce.identity.repository.EmailVerificationTokenRepository;
import com.ecommerce.identity.repository.UserRepository;
import com.ecommerce.identity.security.AccountRateLimitService;
import com.ecommerce.identity.utils.SecureTokenGenerator;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class EmailVerificationConcurrencyIntegrationTest {

  @Autowired private EmailVerificationService emailVerificationService;

  @Autowired private EmailVerificationTokenRepository tokenRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private SecureTokenGenerator secureTokenGenerator;

  @Autowired private TransactionTemplate transactionTemplate;

  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private VerificationEmailSender verificationEmailSender;

    @MockitoBean
    private AccountRateLimitService accountRateLimitService;

  @Test
  void shouldAllowOnlyOneConcurrentVerification() throws Exception {
    String email = "concurrent-" + UUID.randomUUID() + "@example.com";
    String rawToken = secureTokenGenerator.generateToken();

    Long userId =
        transactionTemplate.execute(
            status -> {
              User user =
                  userRepository.saveAndFlush(
                      User.builder()
                          .email(email)
                          .emailNormalized(email)
                          .passwordHash("test-password-hash")
                          .build());

              tokenRepository.saveAndFlush(
                  EmailVerificationToken.builder()
                      .user(user)
                      .tokenHash(secureTokenGenerator.hashToken(rawToken))
                      .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(24))
                      .build());

              return user.getId();
            });

    try {
      CountDownLatch start = new CountDownLatch(1);

      try (var executor = Executors.newFixedThreadPool(2)) {
        var requestA = executor.submit(() -> verify(rawToken, start));
        var requestB = executor.submit(() -> verify(rawToken, start));

        start.countDown();

        boolean resultA = requestA.get(15, TimeUnit.SECONDS);
        boolean resultB = requestB.get(15, TimeUnit.SECONDS);

        assertThat(resultA).isNotEqualTo(resultB);
      }

      User user = userRepository.findById(userId).orElseThrow();

      assertThat(user.getStatus()).isEqualTo(UserStatusEnum.ACTIVE);
      assertThat(user.getEmailVerifiedAt()).isNotNull();

      Integer activationCount =
          jdbcTemplate.queryForObject(
              """
                    SELECT COUNT(*) FROM user_status_history
                    WHERE user_id = ? AND new_status = 'ACTIVE'
                    """,
              Integer.class,
              userId);

      assertThat(activationCount).isEqualTo(1);

      Integer consumedCount =
          jdbcTemplate.queryForObject(
              """
                    SELECT COUNT(*) FROM email_verification_tokens
                    WHERE user_id = ? AND consumed_at IS NOT NULL
                    """,
              Integer.class,
              userId);

      assertThat(consumedCount).isEqualTo(1);
    } finally {
      deleteTestUser(userId);
    }
  }

  private boolean verify(String rawToken, CountDownLatch start) throws InterruptedException {
    start.await();

    try {
      emailVerificationService.verifyEmail(rawToken);
      return true;
    } catch (InvalidVerificationTokenException ex) {
      return false;
    }
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
}
