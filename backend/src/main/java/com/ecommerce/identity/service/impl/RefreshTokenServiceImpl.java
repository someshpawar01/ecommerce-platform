package com.ecommerce.identity.service.impl;

import com.ecommerce.identity.exception.refreshtoken.InvalidRefreshTokenException;
import com.ecommerce.identity.exception.refreshtoken.RefreshTokenExpiredException;
import com.ecommerce.identity.exception.refreshtoken.RefreshTokenReuseException;
import com.ecommerce.identity.exception.refreshtoken.SessionRevokedException;
import com.ecommerce.identity.repository.RefreshTokenSessionRepository;
import com.ecommerce.identity.service.RefreshTokenService;
import com.ecommerce.identity.utils.SecureTokenGenerator;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final RefreshTokenSessionRepository sessionRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final RefreshTokenRotationService rotationService;

    @Override
    public RefreshTokenResult refresh(String rawRefreshToken) {

        if (rawRefreshToken == null || !TOKEN_PATTERN.matcher(rawRefreshToken).matches()) {
            throw new InvalidRefreshTokenException();
        }

        byte[] tokenHash = secureTokenGenerator.hashToken(rawRefreshToken);

        Long userId = sessionRepository.findUserIdByTokenHash(tokenHash)
            .orElseThrow(InvalidRefreshTokenException::new);

        RefreshTokenRotationService.RotationOutcome outcome = rotationService.rotate(userId, tokenHash);

        if (outcome.failure() != null) {
            switch (outcome.failure()) {
                case INVALID -> throw new InvalidRefreshTokenException();
                case EXPIRED -> throw new RefreshTokenExpiredException();
                case REUSED -> throw new RefreshTokenReuseException();
                case REVOKED -> throw new SessionRevokedException();
            }
        }

        return outcome.result();
    }
}
