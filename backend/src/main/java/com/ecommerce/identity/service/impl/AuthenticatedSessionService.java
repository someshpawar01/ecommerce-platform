package com.ecommerce.identity.service.impl;

import com.ecommerce.identity.config.JwtProperties;
import com.ecommerce.identity.dto.response.LoginResponse;
import com.ecommerce.identity.entity.RefreshTokenSession;
import com.ecommerce.identity.entity.User;
import com.ecommerce.identity.exception.AccountClosedException;
import com.ecommerce.identity.exception.AccountNotActiveException;
import com.ecommerce.identity.exception.AccountSuspendedException;
import com.ecommerce.identity.exception.EmailNotVerifiedException;
import com.ecommerce.identity.exception.IdentityConfigurationException;
import com.ecommerce.identity.exception.InvalidCredentialsException;
import com.ecommerce.identity.repository.RefreshTokenSessionRepository;
import com.ecommerce.identity.repository.UserRepository;
import com.ecommerce.identity.security.JwtTokenService;
import com.ecommerce.identity.service.AuthService.LoginResult;
import com.ecommerce.identity.utils.SecureTokenGenerator;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticatedSessionService {

    private static final int REFRESH_INACTIVITY_DAYS = 7;
    private static final int SESSION_ABSOLUTE_DAYS = 30;

    private final UserRepository userRepository;
    private final RefreshTokenSessionRepository sessionRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public LoginResult createAuthenticatedSession(Long userId, String verifiedPasswordHash) {
        User user = userRepository.findByIdForUpdate(userId)
            .orElseThrow(InvalidCredentialsException::new);

        if (!user.getPasswordHash().equals(verifiedPasswordHash)) {
            throw new InvalidCredentialsException();
        }

        validateAccountStatus(user);

        List<String> roles = userRepository.findRoleCodesByUserId(user.getId());
        List<String> permissions = userRepository.findPermissionCodesByUserId(user.getId());

        if (roles.isEmpty()) {
            throw new IdentityConfigurationException("No roles are assigned to the authenticated user");
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

        UUID sessionFamilyId = UUID.randomUUID();
        String refreshToken = secureTokenGenerator.generateToken();

        RefreshTokenSession session = RefreshTokenSession.builder()
            .user(user)
            .tokenHash(secureTokenGenerator.hashToken(refreshToken))
            .sessionFamilyId(sessionFamilyId)
            .expiresAt(now.plusDays(REFRESH_INACTIVITY_DAYS))
            .absoluteExpiresAt(now.plusDays(SESSION_ABSOLUTE_DAYS))
            .build();

        sessionRepository.save(session);

        String accessToken = jwtTokenService.generateAccessToken(user.getId(),
            sessionFamilyId, roles, permissions);

        user.setLastLoginAt(now);

        LoginResponse response = new LoginResponse(
            accessToken,
            "Bearer",
            jwtProperties.accessTokenMinutes() * 60,
            new LoginResponse.UserInfo(user.getId(), user.getEmail(), List.copyOf(roles))
        );

        return new LoginResult(
            response,
            refreshToken,
            REFRESH_INACTIVITY_DAYS * 24L * 60 * 60
        );
    }

    private void validateAccountStatus(User user) {
        switch (user.getStatus()) {
            case UNVERIFIED -> throw new EmailNotVerifiedException();
            case SUSPENDED -> throw new AccountSuspendedException();
            case CLOSED -> throw new AccountClosedException();
            case CLOSURE_PENDING -> throw new AccountNotActiveException();
        }
    }
}
