package com.ecommerce.identity.service.impl;

import com.ecommerce.identity.entity.RefreshTokenSession;
import com.ecommerce.identity.repository.RefreshTokenSessionRepository;
import com.ecommerce.identity.repository.UserRepository;
import com.ecommerce.identity.service.LogoutService;
import com.ecommerce.identity.utils.SecureTokenGenerator;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogoutServiceImpl implements LogoutService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");

    private static final String LOGOUT = "LOGOUT";
    private static final String LOGOUT_ALL = "LOGOUT_ALL";

    private final UserRepository userRepository;
    private final RefreshTokenSessionRepository sessionRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final Clock clock;

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {

        if (rawRefreshToken == null || !TOKEN_PATTERN.matcher(rawRefreshToken).matches()) {
            return;
        }

        byte[] tokenHash = secureTokenGenerator.hashToken(rawRefreshToken);

        Optional<Long> userIdOptional = sessionRepository.findUserIdByTokenHash(tokenHash);

        if (userIdOptional.isEmpty()) {
            return;
        }

        Long userId = userIdOptional.get();

        //This is lock request because of consistent lock mechanism we follow;
        //If refreshToken and logout request comes simultaneously then the problem may arise if this doesn't follow
        //consistent locking
        //If its token has already been rotated, it doesn't revoke the new session.
        if (userRepository.findByIdForUpdate(userId).isEmpty()) {
            return;
        }

        RefreshTokenSession session = sessionRepository.findByTokenHashForUpdate(tokenHash)
            .orElse(null);

        if (session == null || !session.getUser().getId().equals(userId)) {
            return;
        }

        // Only a currently active refresh token can terminate its session family.
        if (session.getRevokedAt() != null) {
            return;
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

        sessionRepository.revokeSessionFamily(session.getSessionFamilyId(), now, LOGOUT);
    }

    @Override
    @Transactional
    public void logoutAll(Long userId) {

        if (userRepository.findByIdForUpdate(userId).isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

        sessionRepository.revokeAllUserSessions(userId, now, LOGOUT_ALL);
    }
}
