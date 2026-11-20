package com.ecommerce.identity.service.impl;

import com.ecommerce.identity.config.PasswordResetProperties;
import com.ecommerce.identity.entity.PasswordResetToken;
import com.ecommerce.identity.entity.User;
import com.ecommerce.identity.enums.UserStatusEnum;
import com.ecommerce.identity.event.PasswordResetRequestedEvent;
import com.ecommerce.identity.exception.passwordreset.InvalidPasswordResetTokenException;
import com.ecommerce.identity.repository.PasswordResetTokenRepository;
import com.ecommerce.identity.repository.RefreshTokenSessionRepository;
import com.ecommerce.identity.repository.UserRepository;
import com.ecommerce.identity.security.AccountRateLimitService;
import com.ecommerce.identity.service.PasswordResetService;
import com.ecommerce.identity.utils.PasswordValidator;
import com.ecommerce.identity.utils.SecureTokenGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final PasswordValidator passwordValidator;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final AccountRateLimitService accountRateLimitService;

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        accountRateLimitService.checkForgotPassword(normalizedEmail);
        // Initial lookup does not acquire a pessimistic lock.
        User existingUser = userRepository.findByEmailNormalized(normalizedEmail).orElse(null);

        if (existingUser == null) {
            return;
        }

        // Maintain user-first locking across authentication workflows.
        User user = entityManager.find(User.class, existingUser.getId(), LockModeType.PESSIMISTIC_WRITE);

        if (user == null || user.getStatus() != UserStatusEnum.ACTIVE) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // Invalidate previously issued unused reset tokens.
        List<PasswordResetToken> existingTokens =
            tokenRepository.findByUser_IdAndConsumedAtIsNullAndRevokedAtIsNull(user.getId());

        for (PasswordResetToken token : existingTokens) {
            token.setRevokedAt(now);
        }

        String rawToken = secureTokenGenerator.generateToken();

        PasswordResetToken resetToken = PasswordResetToken.builder()
            .user(user).tokenHash(secureTokenGenerator.hashToken(rawToken))
            .expiresAt(now.plus(properties.tokenExpiry())).build();

        tokenRepository.save(resetToken);

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), rawToken));
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        passwordValidator.validate(newPassword);

        byte[] tokenHash = secureTokenGenerator.hashToken(rawToken);

        // Locate the token to discover its user. Do not trust its state yet.
        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(InvalidPasswordResetTokenException::new);

        Long userId = token.getUser().getId();

        // Lock user first, matching login / refresh / logout ordering.
        User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);

        if (user == null) {
            throw new InvalidPasswordResetTokenException();
        }

        // Lock and reload the token after acquiring the user lock.
        entityManager.refresh(token, LockModeType.PESSIMISTIC_WRITE);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (token.getConsumedAt() != null
            || token.getRevokedAt() != null
            || !token.getExpiresAt().isAfter(now)
            || user.getStatus() != UserStatusEnum.ACTIVE) {
            throw new InvalidPasswordResetTokenException();
        }

        String newPasswordHash = passwordEncoder.encode(newPassword);

        user.setPasswordHash(newPasswordHash);
        user.setPasswordChangedAt(now);

        token.setConsumedAt(now);

        // Revoke other outstanding reset tokens.
        List<PasswordResetToken> otherTokens =
            tokenRepository.findByUser_IdAndConsumedAtIsNullAndRevokedAtIsNull(userId);

        for (PasswordResetToken otherToken : otherTokens) {
            if (!otherToken.getId().equals(token.getId())) {
                otherToken.setRevokedAt(now);
            }
        }

        // Revoke all authentication sessions so existing JWTs and refresh tokens become unusable.
        refreshTokenSessionRepository.revokeAllUserSessions(userId, now, "PASSWORD_RESET");
    }
}
