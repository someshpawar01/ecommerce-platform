package com.ecommerce.identity.service.impl;

import com.ecommerce.identity.config.EmailVerificationProperties;
import com.ecommerce.identity.entity.EmailVerificationToken;
import com.ecommerce.identity.entity.User;
import com.ecommerce.identity.enums.UserStatusEnum;
import com.ecommerce.identity.repository.EmailVerificationTokenRepository;
import com.ecommerce.identity.repository.UserRepository;
import com.ecommerce.identity.service.EmailVerificationTokenService;
import com.ecommerce.identity.service.ResendVerificationService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResendVerificationServiceImpl implements ResendVerificationService {

  private final UserRepository userRepository;
  private final EmailVerificationTokenRepository tokenRepository;
  private final EmailVerificationTokenService emailVerificationTokenService;
  private final EmailVerificationProperties verificationProperties;

  @Override
  @Transactional
  public void resendVerification(String email) {
    String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

    // 1.aquire lock and fetch user for email
    Optional<User> optionalUser = userRepository.findByEmailNormalizedWithLock(normalizedEmail);

    if (optionalUser.isEmpty()) {
      return;
    }

    User user = optionalUser.get();

    if (user.getStatus() != UserStatusEnum.UNVERIFIED) {
      return;
    }

    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

    // 2.Find the latest token based on createdAt
    Optional<EmailVerificationToken> latestToken =
        tokenRepository.findTopByUser_IdOrderByCreatedAtDesc(user.getId());

    // 3. check 60 seconds cooldown
    if (latestToken.isPresent()) {
      LocalDateTime nextAllowedAt =
          latestToken.get().getCreatedAt().plus(verificationProperties.resendCooldown());

      if (now.isBefore(nextAllowedAt)) {
        return;
      }
    }

    // 4.check how many tokens issued in past 24 hours.
    LocalDateTime windowStart = now.minusDays(1);

    long tokenCount =
        tokenRepository.countByUser_IdAndCreatedAtGreaterThanEqual(user.getId(), windowStart);

    if (tokenCount >= verificationProperties.maxTokensPerDay()) {
      return;
    }

    // previously issued tokens which are not yet consumed or revoked
    List<EmailVerificationToken> previousTokens =
        tokenRepository.findByUser_IdAndConsumedAtIsNullAndRevokedAtIsNull(user.getId());

    for (EmailVerificationToken token : previousTokens) {
      token.setRevokedAt(now);
    }

    emailVerificationTokenService.issueVerificationToken(user);
  }
}
