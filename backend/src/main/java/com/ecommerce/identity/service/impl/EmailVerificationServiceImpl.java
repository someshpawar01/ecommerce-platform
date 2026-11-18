package com.ecommerce.identity.service.impl;

import com.ecommerce.identity.constants.UserStatusReasons;
import com.ecommerce.identity.entity.EmailVerificationToken;
import com.ecommerce.identity.entity.User;
import com.ecommerce.identity.entity.UserStatusHistory;
import com.ecommerce.identity.enums.UserStatusEnum;
import com.ecommerce.identity.exception.InvalidAccountStateException;
import com.ecommerce.identity.exception.InvalidVerificationTokenException;
import com.ecommerce.identity.repository.EmailVerificationTokenRepository;
import com.ecommerce.identity.repository.UserStatusHistoryRepository;
import com.ecommerce.identity.service.EmailVerificationService;
import com.ecommerce.identity.utils.SecureTokenGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

  private final EmailVerificationTokenRepository tokenRepository;
  private final UserStatusHistoryRepository statusHistoryRepository;
  private final SecureTokenGenerator secureTokenGenerator;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public void verifyEmail(String rawToken) {
    byte[] tokenHash = secureTokenGenerator.hashToken(rawToken);

    EmailVerificationToken token =
        tokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(InvalidVerificationTokenException::new);

    Long userId = token.getUser().getId();

    User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);

    if (user == null) {
      throw new InvalidVerificationTokenException();
    }

    entityManager.refresh(token, LockModeType.PESSIMISTIC_WRITE);

    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

    if (token.getConsumedAt() != null
        || token.getRevokedAt() != null
        || !token.getExpiresAt().isAfter(now)) {
      throw new InvalidVerificationTokenException();
    }

    if (user.getStatus() != UserStatusEnum.UNVERIFIED) {
      throw new InvalidAccountStateException("The account is not eligible for email verification");
    }

    user.setEmailVerifiedAt(now);
    user.setStatus(UserStatusEnum.ACTIVE);
    token.setConsumedAt(now);

    UserStatusHistory history =
        UserStatusHistory.builder()
            .user(user)
            .oldStatus(UserStatusEnum.UNVERIFIED)
            .newStatus(UserStatusEnum.ACTIVE)
            .reason(UserStatusReasons.EMAIL_VERIFICATION)
            .build();

    statusHistoryRepository.save(history);
  }
}
