package com.ecommerce.identity.service.impl;

import com.ecommerce.identity.config.EmailVerificationProperties;
import com.ecommerce.identity.entity.EmailVerificationToken;
import com.ecommerce.identity.entity.User;
import com.ecommerce.identity.event.EmailVerificationRequestedEvent;
import com.ecommerce.identity.repository.EmailVerificationTokenRepository;
import com.ecommerce.identity.service.EmailVerificationTokenService;
import com.ecommerce.identity.utils.SecureTokenGenerator;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationTokenServiceImpl implements EmailVerificationTokenService {

  private final SecureTokenGenerator secureTokenGenerator;
  private final EmailVerificationProperties verificationProperties;
  private final ApplicationEventPublisher eventPublisher;
  private final EmailVerificationTokenRepository emailVerificationTokenRepository;

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void issueVerificationToken(User user) {
    String rawToken = secureTokenGenerator.generateToken();
    byte[] tokenHash = secureTokenGenerator.hashToken(rawToken);

    // Persist EmailVerificationToken using your existing entity and repository.
    LocalDateTime expiresAt =
        LocalDateTime.ofInstant(
            Instant.now().plus(verificationProperties.tokenExpiry()), ZoneOffset.UTC);

    EmailVerificationToken tokenEntity =
        EmailVerificationToken.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(expiresAt)
            .build();

    emailVerificationTokenRepository.save(tokenEntity);

    eventPublisher.publishEvent(new EmailVerificationRequestedEvent(user.getEmail(), rawToken));
  }
}
