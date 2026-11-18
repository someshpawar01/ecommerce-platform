package com.ecommerce.identity.controller;

import com.ecommerce.identity.dto.request.ForgotPasswordRequest;
import com.ecommerce.identity.dto.request.LoginRequest;
import com.ecommerce.identity.dto.request.ResetPasswordRequest;
import com.ecommerce.identity.dto.response.CurrentUserResponse;
import com.ecommerce.identity.dto.response.LoginResponse;
import com.ecommerce.identity.dto.response.RefreshTokenResponse;
import com.ecommerce.identity.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final boolean secureCookies;
    private final RefreshTokenService refreshService;
    private final LogoutService logoutService;
    private final CurrentUserService currentUserService;
    private final PasswordResetService passwordResetService;


    public AuthController(AuthService authService,
                          @Value("${app.security.cookie.secure:true}") boolean secureCookies, RefreshTokenService refreshService, LogoutService logoutService, CurrentUserService currentUserService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.secureCookies = secureCookies;
        this.refreshService = refreshService;
        this.logoutService = logoutService;
        this.currentUserService = currentUserService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", result.refreshToken())
            .httpOnly(true)
            .secure(secureCookies)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(result.refreshCookieMaxAgeSeconds())
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header(HttpHeaders.PRAGMA, "no-cache") //legacy; just for compatibility
            .body(result.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
        @CookieValue(value = "refresh_token", required = false) String rawRefreshToken) {

        RefreshTokenService.RefreshTokenResult result = refreshService.refresh(rawRefreshToken);

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", result.refreshToken())
            .httpOnly(true)
            .secure(secureCookies) // https vs http
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(result.refreshCookieMaxAgeSeconds())
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header(HttpHeaders.PRAGMA, "no-cache")
            .body(result.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(value = "refresh_token", required = false) String rawRefreshToken) {

        logoutService.logout(rawRefreshToken);

        ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(secureCookies)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(0)
            .build();

        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.valueOf(jwt.getSubject());

        logoutService.logoutAll(userId);

        ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(secureCookies)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(0)
            .build();

        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.valueOf(jwt.getSubject());

        CurrentUserResponse response = currentUserService.getCurrentUser(userId);

        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request.email());

        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());

        return ResponseEntity.noContent().build();
    }
}
