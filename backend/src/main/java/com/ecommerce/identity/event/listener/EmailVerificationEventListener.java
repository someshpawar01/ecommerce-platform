package com.ecommerce.identity.event.listener;

import com.ecommerce.identity.config.EmailVerificationProperties;
import com.ecommerce.identity.event.EmailVerificationRequestedEvent;
import com.ecommerce.identity.service.VerificationEmailSender;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationEventListener {

  private final VerificationEmailSender verificationEmailSender;
  private final EmailVerificationProperties verificationProperties;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleEmailVerificationRequested(EmailVerificationRequestedEvent event) {
    String encodedToken = URLEncoder.encode(event.rawToken(), StandardCharsets.UTF_8);
    String verificationLink =
        verificationProperties.baseUrl() + "/verify-email?token=" + encodedToken;

    try {
      verificationEmailSender.sendVerificationEmail(event.email(), verificationLink);
    } catch (Exception ex) {
      log.error("Failed to dispatch verification email after registration commit", ex);
    }
  }
}
