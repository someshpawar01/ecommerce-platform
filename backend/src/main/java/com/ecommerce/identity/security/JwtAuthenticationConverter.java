package com.ecommerce.identity.security;

import com.ecommerce.identity.entity.User;
import com.ecommerce.identity.enums.UserStatusEnum;
import com.ecommerce.identity.repository.RefreshTokenSessionRepository;
import com.ecommerce.identity.repository.UserRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;
    private final RefreshTokenSessionRepository sessionRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {

        Long userId;

        try {
            userId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException | NullPointerException exception) {
            throw new InvalidBearerTokenException("Invalid access token");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new InvalidBearerTokenException("Invalid access token"));

        if (user.getStatus() != UserStatusEnum.ACTIVE) {
            throw new InvalidBearerTokenException("Invalid access token");
        }

        UUID sessionFamilyId = UUID.fromString(jwt.getClaimAsString("sid"));

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

        boolean activeSession = sessionRepository
            .existsBySessionFamilyIdAndUser_IdAndRevokedAtIsNullAndExpiresAtAfterAndAbsoluteExpiresAtAfter(
                sessionFamilyId, userId, now, now);

        if (!activeSession) {
            throw new InvalidBearerTokenException("Invalid access token");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();

        userRepository.findRoleCodesByUserId(userId)
            .forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));

        userRepository.findPermissionCodesByUserId(userId)
            .forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
