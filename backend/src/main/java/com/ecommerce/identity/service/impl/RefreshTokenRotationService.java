package com.ecommerce.identity.service.impl;

import com.ecommerce.identity.config.JwtProperties;
import com.ecommerce.identity.dto.response.RefreshTokenResponse;
import com.ecommerce.identity.entity.RefreshTokenSession;
import com.ecommerce.identity.entity.User;
import com.ecommerce.identity.enums.UserStatusEnum;
import com.ecommerce.identity.repository.RefreshTokenSessionRepository;
import com.ecommerce.identity.repository.UserRepository;
import com.ecommerce.identity.security.JwtTokenService;
import com.ecommerce.identity.service.RefreshTokenService.RefreshTokenResult;
import com.ecommerce.identity.utils.SecureTokenGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenRotationService {

    private static final int INACTIVITY_DAYS = 7;

    private final UserRepository userRepository;
    private final RefreshTokenSessionRepository sessionRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public RotationOutcome rotate(Long userId, byte[] tokenHash) {

        // Lock the user first. Other session-changing operations must follow this lock order.
        User user = userRepository.findByIdForUpdate(userId).orElse(null);

        if (user == null || user.getStatus() != UserStatusEnum.ACTIVE) {
            return RotationOutcome.failed(RefreshTokenFailure.INVALID);
        }

        RefreshTokenSession current = sessionRepository.findByTokenHashForUpdate(tokenHash)
            .orElse(null);

        if (current == null || !current.getUser().getId().equals(userId)) {
            return RotationOutcome.failed(RefreshTokenFailure.INVALID);
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

        if (current.getRevokedAt() != null) {
            if ("ROTATED".equals(current.getRevokeReason())) {
                sessionRepository.revokeSessionFamily(current.getSessionFamilyId(), now, "TOKEN_REUSE");
                return RotationOutcome.failed(RefreshTokenFailure.REUSED);
            }

            return RotationOutcome.failed(RefreshTokenFailure.REVOKED);
        }

        if (!current.getExpiresAt().isAfter(now) || !current.getAbsoluteExpiresAt().isAfter(now)) {
            return RotationOutcome.failed(RefreshTokenFailure.EXPIRED);
        }

        List<String> roles = userRepository.findRoleCodesByUserId(userId);
        List<String> permissions = userRepository.findPermissionCodesByUserId(userId);

        if (roles.isEmpty()) {
            return RotationOutcome.failed(RefreshTokenFailure.INVALID);
        }

        String replacementToken = secureTokenGenerator.generateToken();

        LocalDateTime inactivityExpiry = now.plusDays(INACTIVITY_DAYS);
        // existing inactivity + 7 days VS 30 days absolute expiry check
        LocalDateTime replacementExpiry = inactivityExpiry.isBefore(current.getAbsoluteExpiresAt())
            ? inactivityExpiry
            : current.getAbsoluteExpiresAt();

        current.setRevokedAt(now);
        current.setLastUsedAt(now);
        current.setRevokeReason("ROTATED");

        RefreshTokenSession replacement = RefreshTokenSession.builder()
            .user(user)
            .tokenHash(secureTokenGenerator.hashToken(replacementToken))
            .sessionFamilyId(current.getSessionFamilyId())
            .expiresAt(replacementExpiry)
            .absoluteExpiresAt(current.getAbsoluteExpiresAt())
            .rotatedFromSession(current)
            .build();

        sessionRepository.save(replacement);

        String accessToken = jwtTokenService.generateAccessToken(
            userId, current.getSessionFamilyId(), roles, permissions);

        long cookieMaxAge = Duration.between(
            now.toInstant(ZoneOffset.UTC),
            replacementExpiry.toInstant(ZoneOffset.UTC)
        ).toSeconds();

        RefreshTokenResponse response = new RefreshTokenResponse(
            accessToken,
            "Bearer",
            jwtProperties.accessTokenMinutes() * 60
        );

        return RotationOutcome.success(new RefreshTokenResult(response, replacementToken, cookieMaxAge));
    }

    public enum RefreshTokenFailure {
        INVALID,
        EXPIRED,
        REUSED,
        REVOKED
    }

    public record RotationOutcome(
        RefreshTokenResult result,
        RefreshTokenFailure failure
    ) {

        //factory methods: here used only for readability purposes.
        public static RotationOutcome success(RefreshTokenResult result) {
            return new RotationOutcome(result, null);
        }

        public static RotationOutcome failed(RefreshTokenFailure failure) {
            return new RotationOutcome(null, failure);
        }
    }
}
