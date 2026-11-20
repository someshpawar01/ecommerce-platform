package com.ecommerce.identity.service.impl;

import com.ecommerce.identity.dto.projection.LoginCredentialsProjection;
import com.ecommerce.identity.dto.request.LoginRequest;
import com.ecommerce.identity.exception.InvalidCredentialsException;
import com.ecommerce.identity.repository.UserRepository;
import com.ecommerce.identity.security.AccountRateLimitService;
import com.ecommerce.identity.service.AuthService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedSessionService authenticatedSessionService;
    private final AccountRateLimitService accountRateLimitService;

    @Override
    public LoginResult login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        accountRateLimitService.checkLogin(normalizedEmail);

        //To keep the heavy password hash match outside of transaction
        //and to overcome the persistent context issue of maintaining user entity and
        //returning same entity again after aquiring lock will not allow to
        //reliably verify the password.
        LoginCredentialsProjection credentials = userRepository.findLoginCredentials(normalizedEmail)
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), credentials.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        return authenticatedSessionService.createAuthenticatedSession(
            credentials.id(),
            credentials.passwordHash()
        );
    }
}
