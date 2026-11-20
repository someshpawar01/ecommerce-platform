package com.ecommerce.identity.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.identity.security.AccountRateLimitService;
import com.ecommerce.identity.service.VerificationEmailSender;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationEmailDispatchIntegrationTest {

  private static final String REGISTER_URL = "/api/v1/auth/register";

  @Autowired private MockMvc mockMvc;

  @Autowired private ApplicationEventPublisher eventPublisher;

  @Autowired private TransactionTemplate transactionTemplate;

  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private VerificationEmailSender verificationEmailSender;

    @MockitoBean
    private AccountRateLimitService accountRateLimitService;

  @Test
  void shouldSendVerificationEmailAfterSuccessfulRegistration() throws Exception {
    String email = "dispatch-" + UUID.randomUUID() + "@example.com";

    String requestBody =
        """
                {
                    "firstName": "Rahul",
                    "lastName": "Sharma",
                    "email": "%s",
                    "password": "MySecurePassword123!"
                }
                """
            .formatted(email);

    try {
      mockMvc
          .perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(requestBody))
          .andExpect(status().isCreated());

      Integer userCount =
          jdbcTemplate.queryForObject(
              """
                    SELECT COUNT(*) FROM users WHERE email_normalized = ?
                    """,
              Integer.class,
              email);

      assertThat(userCount).isEqualTo(1);

      verify(verificationEmailSender, times(1))
          .sendVerificationEmail(eq(email), contains("/verify-email?token="));
    } finally {
      deleteTestUser(email);
    }
  }

  @Test
  void shouldNotSendEmailWhenTransactionRollsBack() {
    String email = "rollback-" + UUID.randomUUID() + "@example.com";

    assertThatThrownBy(
            () ->
                transactionTemplate.executeWithoutResult(
                    status -> {
                      eventPublisher.publishEvent(
                          new EmailVerificationRequestedEvent(email, "sample-token"));
                      throw new IllegalStateException("Simulated transaction failure");
                    }))
        .isInstanceOf(IllegalStateException.class);

    verifyNoInteractions(verificationEmailSender);
  }

  @Test
  void shouldSendEmailOnlyAfterCommit() {
    String email = "commit-" + UUID.randomUUID() + "@example.com";

    transactionTemplate.executeWithoutResult(
        status -> {
          eventPublisher.publishEvent(new EmailVerificationRequestedEvent(email, "sample-token"));

          verifyNoInteractions(verificationEmailSender);
        });

    verify(verificationEmailSender, times(1))
        .sendVerificationEmail(eq(email), contains("/verify-email?token="));
  }

  private void deleteTestUser(String email) {
    Long userId =
        jdbcTemplate.query(
            """
                SELECT id FROM users WHERE email_normalized = ?
                """,
            rs -> rs.next() ? rs.getLong(1) : null,
            email);

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
