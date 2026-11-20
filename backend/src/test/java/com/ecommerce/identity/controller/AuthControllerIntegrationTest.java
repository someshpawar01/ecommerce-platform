package com.ecommerce.identity.controller;

import com.ecommerce.identity.constants.RoleCodes;
import com.ecommerce.identity.entity.*;
import com.ecommerce.identity.enums.UserStatusEnum;
import com.ecommerce.identity.repository.*;
import com.ecommerce.identity.security.AccountRateLimitService;
import com.ecommerce.identity.service.PasswordResetEmailSender;
import com.ecommerce.identity.utils.SecureTokenGenerator;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthControllerIntegrationTest.TestAuthorizationConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private RefreshTokenSessionRepository sessionRepository;

    @Autowired
    private SecureTokenGenerator secureTokenGenerator;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @MockitoBean
    private PasswordResetEmailSender passwordResetEmailSender;

    @MockitoBean
    private AccountRateLimitService accountRateLimitService;

    @Autowired
    private Clock clock;

    private final List<Long> createdUserIds = new ArrayList<>();


    @AfterEach
    void cleanup() {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                for (Long userId : createdUserIds) {

                    // 1. Remove self-referencing refresh-token relationships.
                    entityManager.createQuery("""
                        UPDATE RefreshTokenSession s
                        SET s.rotatedFromSession = null
                        WHERE s.user.id = :userId
                        """)
                        .setParameter("userId", userId)
                        .executeUpdate();

                    // 2. Delete all refresh-token sessions belonging to this user.
                    entityManager.createQuery("""
                        DELETE FROM RefreshTokenSession s
                        WHERE s.user.id = :userId
                        """)
                        .setParameter("userId", userId)
                        .executeUpdate();

                    // 3. Delete password-reset tokens belonging to this user.
                    entityManager.createQuery("""
                        DELETE FROM PasswordResetToken t
                        WHERE t.user.id = :userId
                        """)
                        .setParameter("userId", userId)
                        .executeUpdate();

                    // 4. Delete the user's role assignments.
                    entityManager.createQuery("""
                        DELETE FROM UserRole ur
                        WHERE ur.user.id = :userId
                        """)
                        .setParameter("userId", userId)
                        .executeUpdate();

                    // 5. Delete the test user.
                    entityManager.createQuery("""
                        DELETE FROM User u
                        WHERE u.id = :userId
                        """)
                        .setParameter("userId", userId)
                        .executeUpdate();
                }
            });
        } finally {
            createdUserIds.clear();
        }
    }


    @Test
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
        Cookie csrfCookie = obtainCsrfCookie();

        String requestBody = """
            {
                "email": "nonexistent@example.com",
                "password": "WrongPassword123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLoginWithoutCsrfToken() throws Exception {
        String requestBody = """
            {
                "email": "nonexistent@example.com",
                "password": "WrongPassword123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectLoginWithMalformedJson() throws Exception {
        Cookie csrfCookie = obtainCsrfCookie();

        mockMvc.perform(post("/api/v1/auth/login")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid-json"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectCurrentUserRequestWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectCurrentUserRequestWithInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer invalid.jwt.token"))
            .andExpect(status().isUnauthorized());
    }

    private Cookie obtainCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isNoContent())
            .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");

        assertNotNull(csrfCookie, "Expected XSRF-TOKEN cookie from CSRF endpoint");

        return csrfCookie;
    }

    @Test
    void shouldLoginAndAccessCurrentUserWithGeneratedJwt() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        User user = createActiveCustomer(email, password);

        MvcResult loginResult = performLogin(email, password);

        assertEquals(200, loginResult.getResponse().getStatus());

        String accessToken = extractAccessToken(loginResult);
        Cookie refreshCookie = extractRefreshCookie(loginResult);

        assertFalse(accessToken.isBlank());
        assertEquals("/api/v1/auth", refreshCookie.getPath());

        Jwt jwt = jwtDecoder.decode(accessToken);

        assertEquals(user.getId().toString(), jwt.getSubject());
        assertEquals("access", jwt.getClaimAsString("token_type"));
        assertNotNull(jwt.getClaimAsString("sid"));

        assertTrue(jwt.getClaimAsStringList("roles").contains(RoleCodes.CUSTOMER));

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId()))
            .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void shouldRejectLoginWithIncorrectPassword() throws Exception {
        String email = "invalid-password-" + UUID.randomUUID() + "@example.com";

        createActiveCustomer(email, "CorrectPassword123!");

        MvcResult result = performLogin(email, "WrongPassword123!");

        assertEquals(401, result.getResponse().getStatus());
    }

    @Test
    void shouldRejectLoginForUnverifiedCustomer() throws Exception {
        String email = "unverified-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        User user = createActiveCustomer(email, password);

        transactionTemplate.executeWithoutResult(status -> {
            User managedUser = userRepository.findById(user.getId()).orElseThrow();
            managedUser.setStatus(UserStatusEnum.UNVERIFIED);
            managedUser.setEmailVerifiedAt(null);
        });

        MvcResult result = performLogin(email, password);

        assertEquals(401, result.getResponse().getStatus());

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        assertEquals("INVALID_CREDENTIALS", response.path("code").asText());
        assertEquals("Authentication failed", response.path("detail").asText());
    }

    @Test
    void shouldRotateRefreshTokenSuccessfully() throws Exception {
        String email = "refresh-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        User user = createActiveCustomer(email, password);

        MvcResult loginResult = performLogin(email, password);

        assertEquals(200, loginResult.getResponse().getStatus());

        Cookie originalRefreshCookie = extractRefreshCookie(loginResult);

        MvcResult refreshResult = performRefresh(originalRefreshCookie);

        assertEquals(200, refreshResult.getResponse().getStatus());

        Cookie rotatedRefreshCookie = extractRefreshCookie(refreshResult);

        RefreshTokenSession originalSession = findSession(originalRefreshCookie.getValue());
        RefreshTokenSession rotatedSession = findSession(rotatedRefreshCookie.getValue());

        assertNotNull(originalSession.getRevokedAt());
        assertEquals("ROTATED", originalSession.getRevokeReason());

        assertNull(rotatedSession.getRevokedAt());
        assertEquals(originalSession.getSessionFamilyId(), rotatedSession.getSessionFamilyId());

        assertNotNull(rotatedSession.getExpiresAt());
        assertNotNull(rotatedSession.getAbsoluteExpiresAt());

        assertEquals(originalSession.getId(), rotatedSession.getRotatedFromSession().getId());

        assertNotEquals(originalRefreshCookie.getValue(), rotatedRefreshCookie.getValue());

        String newAccessToken = extractAccessToken(refreshResult);

        Jwt jwt = jwtDecoder.decode(newAccessToken);

        assertEquals(user.getId().toString(), jwt.getSubject());
        assertEquals("access", jwt.getClaimAsString("token_type"));

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken))
            .andExpect(status().isOk());
    }

    @Test
    void shouldRevokeSessionFamilyWhenRotatedTokenIsReused() throws Exception {
        String email = "replay-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        createActiveCustomer(email, password);

        MvcResult loginResult = performLogin(email, password);

        assertEquals(200, loginResult.getResponse().getStatus());

        Cookie originalRefreshCookie = extractRefreshCookie(loginResult);

        MvcResult firstRefresh = performRefresh(originalRefreshCookie);

        assertEquals(200, firstRefresh.getResponse().getStatus());

        Cookie rotatedRefreshCookie = extractRefreshCookie(firstRefresh);

        String newAccessToken = extractAccessToken(firstRefresh);

        // Reuse the original refresh token.
        MvcResult reuseResult = performRefresh(originalRefreshCookie);

        assertEquals(401, reuseResult.getResponse().getStatus());

        RefreshTokenSession revokedSession = findSession(rotatedRefreshCookie.getValue());

        assertNotNull(revokedSession.getRevokedAt());
        assertEquals("TOKEN_REUSE", revokedSession.getRevokeReason());

        // The newly issued refresh token should now be unusable.
        MvcResult subsequentRefresh = performRefresh(rotatedRefreshCookie);

        assertEquals(401, subsequentRefresh.getResponse().getStatus());

        // The JWT from the revoked session family should also be rejected.
        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldInvalidateJwtAndRefreshTokenAfterLogout() throws Exception {
        String email = "logout-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        createActiveCustomer(email, password);

        MvcResult loginResult = performLogin(email, password);

        assertEquals(200, loginResult.getResponse().getStatus());

        String accessToken = extractAccessToken(loginResult);
        Cookie refreshCookie = extractRefreshCookie(loginResult);

        Cookie csrfCookie = obtainCsrfCookie();

        MvcResult logoutResult = mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(csrfCookie, refreshCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue()))
            .andExpect(status().isNoContent())
            .andReturn();

        RefreshTokenSession revokedSession = findSession(refreshCookie.getValue());

        assertNotNull(revokedSession.getRevokedAt());
        assertEquals("LOGOUT", revokedSession.getRevokeReason());

        Cookie clearedRefreshCookie = logoutResult.getResponse().getCookie("refresh_token");

        assertNotNull(clearedRefreshCookie);
        assertEquals(0, clearedRefreshCookie.getMaxAge());

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isUnauthorized());

        MvcResult refreshResult = performRefresh(refreshCookie);

        assertEquals(401, refreshResult.getResponse().getStatus());
    }

    @Test
    void shouldInvalidateAllSessionsAfterLogoutAll() throws Exception {
        String email = "logout-all-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        createActiveCustomer(email, password);

        // Simulate login from device A.
        MvcResult firstLogin = performLogin(email, password);

        assertEquals(200, firstLogin.getResponse().getStatus());

        String firstAccessToken = extractAccessToken(firstLogin);
        Cookie firstRefreshCookie = extractRefreshCookie(firstLogin);

        // Simulate login from device B.
        MvcResult secondLogin = performLogin(email, password);

        assertEquals(200, secondLogin.getResponse().getStatus());

        String secondAccessToken = extractAccessToken(secondLogin);
        Cookie secondRefreshCookie = extractRefreshCookie(secondLogin);

        // Verify both sessions are independently authenticated.
        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstAccessToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondAccessToken))
            .andExpect(status().isOk());

        Cookie csrfCookie = obtainCsrfCookie();

        // Log out all sessions using device B.
        mockMvc.perform(post("/api/v1/auth/logout-all")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondAccessToken)
                .cookie(csrfCookie, secondRefreshCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue()))
            .andExpect(status().isNoContent());

        // Device A JWT is now invalid.
        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstAccessToken))
            .andExpect(status().isUnauthorized());

        // Device B JWT is now invalid.
        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondAccessToken))
            .andExpect(status().isUnauthorized());

        assertEquals("LOGOUT_ALL", findSession(firstRefreshCookie.getValue()).getRevokeReason());
        assertEquals("LOGOUT_ALL", findSession(secondRefreshCookie.getValue()).getRevokeReason());
        // Neither device can obtain another JWT.
        assertEquals(401, performRefresh(firstRefreshCookie).getResponse().getStatus());
        assertEquals(401, performRefresh(secondRefreshCookie).getResponse().getStatus());
    }

    @Test
    void shouldAllowRepeatedLogout() throws Exception {
        String email = "repeated-logout-" + UUID.randomUUID() + "@example.com";

        createActiveCustomer(email, "TestPassword123!");

        MvcResult loginResult = performLogin(email, "TestPassword123!");

        assertEquals(200, loginResult.getResponse().getStatus());

        Cookie refreshCookie = extractRefreshCookie(loginResult);

        Cookie csrfCookie = obtainCsrfCookie();

        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(csrfCookie, refreshCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue()))
            .andExpect(status().isNoContent());

        // Repeat logout using the old cookie.
        csrfCookie = obtainCsrfCookie();

        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(csrfCookie, refreshCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue()))
            .andExpect(status().isNoContent());
    }

    private RefreshTokenSession findSession(String rawRefreshToken) {
        byte[] tokenHash = secureTokenGenerator.hashToken(rawRefreshToken);

        return sessionRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new AssertionError("Refresh session not found"));
    }

    private User createActiveCustomer(String email, String password) {
        User user = transactionTemplate.execute(status -> {
            Role customerRole = roleRepository.findByCode(RoleCodes.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role is missing"));

            User customer = User.builder()
                .email(email)
                .emailNormalized(email.toLowerCase(Locale.ROOT))
                .passwordHash(passwordEncoder.encode(password))
                .status(UserStatusEnum.ACTIVE)
                .emailVerifiedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

            customer = userRepository.saveAndFlush(customer);

            userRoleRepository.saveAndFlush(UserRole.builder()
                .user(customer)
                .role(customerRole)
                .build());

            return customer;
        });

        createdUserIds.add(user.getId());
        return user;
    }

    private MvcResult performLogin(String email, String password) throws Exception {
        Cookie csrfCookie = obtainCsrfCookie();

        String requestBody = """
            {
                "email": "%s",
                "password": "%s"
            }
            """.formatted(email, password);

        return mockMvc.perform(post("/api/v1/auth/login")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andReturn();
    }

    private String extractAccessToken(MvcResult result) throws UnsupportedEncodingException {
        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return response.path("accessToken").asText();
    }

    private Cookie extractRefreshCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("refresh_token");

        assertNotNull(cookie, "Refresh token cookie should be present");
        assertFalse(cookie.getValue().isBlank());
        assertTrue(cookie.isHttpOnly());

        return cookie;
    }

    private MvcResult performRefresh(Cookie refreshCookie) throws Exception {
        Cookie csrfCookie = obtainCsrfCookie();

        return mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(csrfCookie, refreshCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue()))
            .andReturn();
    }

    private void expireRefreshSession(String rawRefreshToken) {
        transactionTemplate.executeWithoutResult(status -> {
            byte[] hash = secureTokenGenerator.hashToken(rawRefreshToken);

            RefreshTokenSession session = sessionRepository.findByTokenHash(hash).orElseThrow();

            LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

            LocalDateTime createdAt = now.minusDays(8);
            LocalDateTime expiresAt = now.minusDays(1);

            entityManager.createNativeQuery("""
                    UPDATE refresh_token_sessions
                    SET created_at = :createdAt,
                        expires_at = :expiresAt
                    WHERE id = :sessionId
                    """)
                .setParameter("createdAt", createdAt)
                .setParameter("expiresAt", expiresAt)
                .setParameter("sessionId", session.getId())
                .executeUpdate();
        });
    }

    private void expireAbsoluteSessionLifetime(String rawRefreshToken) {
        transactionTemplate.executeWithoutResult(status -> {
            byte[] hash = secureTokenGenerator.hashToken(rawRefreshToken);

            RefreshTokenSession session = sessionRepository.findByTokenHash(hash).orElseThrow();

            LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

            entityManager.createNativeQuery("""
                    UPDATE refresh_token_sessions
                    SET created_at = :createdAt,
                        expires_at = :expiresAt,
                        absolute_expires_at = :absoluteExpiresAt
                    WHERE id = :sessionId
                    """)
                .setParameter("createdAt", now.minusDays(30))
                .setParameter("expiresAt", now.plusDays(1))
                .setParameter("absoluteExpiresAt", now.minusSeconds(1))
                .setParameter("sessionId", session.getId())
                .executeUpdate();
        });
    }

    @Test
    void shouldRejectExistingJwtAfterAccountSuspension() throws Exception {
        String email = "suspended-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        User user = createActiveCustomer(email, password);

        MvcResult loginResult = performLogin(email, password);
        assertEquals(200, loginResult.getResponse().getStatus());

        String accessToken = extractAccessToken(loginResult);

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk());

        // Simulate the database change that the future admin service will make.
        transactionTemplate.executeWithoutResult(status -> {
            User managedUser = userRepository.findById(user.getId()).orElseThrow();
            managedUser.setStatus(UserStatusEnum.SUSPENDED);
        });

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldUseUpdatedDatabaseRolesInsteadOfOldJwtRoleClaims() throws Exception {
        String email = "role-change-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        User user = createActiveCustomer(email, password);

        MvcResult loginResult = performLogin(email, password);
        assertEquals(200, loginResult.getResponse().getStatus());

        String accessToken = extractAccessToken(loginResult);
        Jwt originalJwt = jwtDecoder.decode(accessToken);

        assertTrue(originalJwt.getClaimAsStringList("roles").contains(RoleCodes.CUSTOMER));

        // Simulate removing the CUSTOMER role from the user.
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createQuery("""
                    DELETE FROM UserRole ur
                    WHERE ur.user.id = :userId
                      AND ur.role.code = :roleCode
                    """)
                .setParameter("userId", user.getId())
                .setParameter("roleCode", RoleCodes.CUSTOMER)
                .executeUpdate();
        });

        // The signed JWT remains unchanged.
        Jwt decodedAgain = jwtDecoder.decode(accessToken);

        assertTrue(decodedAgain.getClaimAsStringList("roles").contains(RoleCodes.CUSTOMER));

        // But /auth/me loads the current roles from MySQL.
        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles").isEmpty());
    }

    @Test
    void shouldRejectJwtWhenRefreshSessionHasExpired() throws Exception {
        String email = "session-expired-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        createActiveCustomer(email, password);

        MvcResult loginResult = performLogin(email, password);
        assertEquals(200, loginResult.getResponse().getStatus());

        String accessToken = extractAccessToken(loginResult);
        Cookie refreshCookie = extractRefreshCookie(loginResult);

        // Confirm the JWT initially works.
        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk());

        // Simulate expiry of the backing refresh session.
        expireRefreshSession(refreshCookie.getValue());

        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRefreshTokenAfterInactivityExpiration() throws Exception {
        String email = "inactivity-expired-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        createActiveCustomer(email, password);

        MvcResult loginResult = performLogin(email, password);
        assertEquals(200, loginResult.getResponse().getStatus());

        Cookie refreshCookie = extractRefreshCookie(loginResult);

        expireRefreshSession(refreshCookie.getValue());

        MvcResult refreshResult = performRefresh(refreshCookie);

        assertEquals(401, refreshResult.getResponse().getStatus());

        RefreshTokenSession session = findSession(refreshCookie.getValue());

        // An expired token must not have been successfully rotated.
        assertNull(session.getRotatedFromSession());
    }

    @Test
    void shouldRejectRefreshTokenAfterAbsoluteExpiration() throws Exception {
        String email = "absolute-expired-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        createActiveCustomer(email, password);

        MvcResult loginResult = performLogin(email, password);
        assertEquals(200, loginResult.getResponse().getStatus());

        Cookie refreshCookie = extractRefreshCookie(loginResult);

        expireAbsoluteSessionLifetime(refreshCookie.getValue());

        MvcResult refreshResult = performRefresh(refreshCookie);

        assertEquals(401, refreshResult.getResponse().getStatus());

        Long sessionId = findSession(refreshCookie.getValue()).getId();

        Long replacementCount = transactionTemplate.execute(status ->
            entityManager.createQuery("""
                    SELECT COUNT(s)
                    FROM RefreshTokenSession s
                    WHERE s.rotatedFromSession.id = :sessionId
                    """, Long.class)
                .setParameter("sessionId", sessionId)
                .getSingleResult());

        assertEquals(0L, replacementCount);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestAuthorizationConfig {

        @Bean
        TestPermissionController testPermissionController() {
            return new TestPermissionController();
        }
    }

    @RestController
    public static class TestPermissionController {

        @PreAuthorize("hasRole('CUSTOMER')")
        @GetMapping("/test/auth/customer-only")
        String customerOnly() {
            return "Allowed";
        }
    }

    @Test
    void shouldDenyProtectedOperationAfterRoleRemoval() throws Exception {
        String email = "authorization-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        User user = createActiveCustomer(email, password);

        MvcResult loginResult = performLogin(email, password);
        assertEquals(200, loginResult.getResponse().getStatus());

        String accessToken = extractAccessToken(loginResult);

        // CUSTOMER role exists, so access is allowed.
        mockMvc.perform(get("/test/auth/customer-only")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk());

        // Remove only this test user's role assignment.
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createQuery("""
                    DELETE FROM UserRole ur
                    WHERE ur.user.id = :userId
                    """)
                .setParameter("userId", user.getId())
                .executeUpdate();
        });

        // The same JWT must now be denied.
        mockMvc.perform(get("/test/auth/customer-only")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void shouldPreventConcurrentRefreshTokenRotation() throws Exception {
        String email = "concurrent-refresh-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        createActiveCustomer(email, password);

        MvcResult loginResult = performLogin(email, password);
        assertEquals(200, loginResult.getResponse().getStatus());

        Cookie originalRefreshCookie = extractRefreshCookie(loginResult);

        Cookie csrfCookieA = obtainCsrfCookie();
        Cookie csrfCookieB = obtainCsrfCookie();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

            Callable<MvcResult> firstRequest = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start");
                }

                return mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(csrfCookieA, originalRefreshCookie)
                        .header("X-XSRF-TOKEN", csrfCookieA.getValue()))
                    .andReturn();
            };

            Callable<MvcResult> secondRequest = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start");
                }

                return mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(csrfCookieB, originalRefreshCookie)
                        .header("X-XSRF-TOKEN", csrfCookieB.getValue()))
                    .andReturn();
            };

            Future<MvcResult> first = executor.submit(firstRequest);
            Future<MvcResult> second = executor.submit(secondRequest);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            MvcResult firstResult = first.get(20, TimeUnit.SECONDS);
            MvcResult secondResult = second.get(20, TimeUnit.SECONDS);

            int firstStatus = firstResult.getResponse().getStatus();
            int secondStatus = secondResult.getResponse().getStatus();

            // One rotation succeeds, the other detects reuse.
            assertTrue((firstStatus == 200 && secondStatus == 401)
                || (firstStatus == 401 && secondStatus == 200));

            MvcResult successfulResult = firstStatus == 200 ? firstResult : secondResult;

            Cookie rotatedRefreshCookie = extractRefreshCookie(successfulResult);

            RefreshTokenSession original = findSession(originalRefreshCookie.getValue());
            RefreshTokenSession replacement = findSession(rotatedRefreshCookie.getValue());

            assertEquals("ROTATED", original.getRevokeReason());
            assertEquals(original.getSessionFamilyId(), replacement.getSessionFamilyId());

            // Strict replay detection must revoke the replacement token too.
            assertEquals("TOKEN_REUSE", replacement.getRevokeReason());
            assertNotNull(replacement.getRevokedAt());
        }
    }

    private MvcResult performForgotPassword(String email) throws Exception {
        Cookie csrfCookie = obtainCsrfCookie();

        String requestBody = """
            {
                "email": "%s"
            }
            """.formatted(email);

        return mockMvc.perform(post("/api/v1/auth/forgot-password")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andReturn();
    }

    private MvcResult performResetPassword(String token, String newPassword) throws Exception {
        Cookie csrfCookie = obtainCsrfCookie();

        String requestBody = """
            {
                "token": "%s",
                "newPassword": "%s"
            }
            """.formatted(token, newPassword);

        return mockMvc.perform(post("/api/v1/auth/reset-password")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andReturn();
    }

    @Test
    void shouldReturnAcceptedForUnknownEmail() throws Exception {
        String email = "unknown-" + UUID.randomUUID() + "@example.com";

        MvcResult result = performForgotPassword(email);

        assertEquals(202, result.getResponse().getStatus());

        verifyNoInteractions(passwordResetEmailSender);
    }

    @Test
    void shouldIssuePasswordResetTokenForActiveUser() throws Exception {
        String email = "forgot-" + UUID.randomUUID() + "@example.com";
        User user = createActiveCustomer(email, "OldPassword123!");

        MvcResult result = performForgotPassword(email);

        assertEquals(202, result.getResponse().getStatus());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);

        verify(passwordResetEmailSender).sendPasswordResetEmail(eq(email), tokenCaptor.capture());

        String rawToken = extractRawResetToken(tokenCaptor.getValue());

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());

        byte[] tokenHash = secureTokenGenerator.hashToken(rawToken);

        PasswordResetToken savedToken = tokenRepository.findByTokenHash(tokenHash).orElseThrow();

        assertEquals(user.getId(), savedToken.getUser().getId());
        assertNull(savedToken.getConsumedAt());
        assertNull(savedToken.getRevokedAt());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void shouldResetPasswordSuccessfully() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        String oldPassword = "OldPassword123!";
        String newPassword = "NewPassword123!";

        User user = createActiveCustomer(email, oldPassword);

        performForgotPassword(email);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetEmailSender).sendPasswordResetEmail(eq(email), tokenCaptor.capture());

        String rawToken = extractRawResetToken(tokenCaptor.getValue());

        MvcResult resetResult = performResetPassword(rawToken, newPassword);

        assertEquals(204, resetResult.getResponse().getStatus());

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPasswordHash()));
        assertFalse(passwordEncoder.matches(oldPassword, updatedUser.getPasswordHash()));
        assertNotNull(updatedUser.getPasswordChangedAt());

        PasswordResetToken token = tokenRepository
            .findByTokenHash(secureTokenGenerator.hashToken(rawToken))
            .orElseThrow();

        assertNotNull(token.getConsumedAt());
    }

    @Test
    void shouldRejectAlreadyConsumedResetToken() throws Exception {
        String email = "reuse-reset-" + UUID.randomUUID() + "@example.com";

        createActiveCustomer(email, "OldPassword123!");

        performForgotPassword(email);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetEmailSender).sendPasswordResetEmail(eq(email), tokenCaptor.capture());

        String rawToken = extractRawResetToken(tokenCaptor.getValue());

        assertEquals(204, performResetPassword(rawToken, "NewPassword123!").getResponse().getStatus());

        MvcResult reusedResult = performResetPassword(rawToken, "AnotherPassword123!");

        assertEquals(400, reusedResult.getResponse().getStatus());
    }

    @Test
    void shouldRevokePreviousResetTokenWhenAnotherIsRequested() throws Exception {
        String email = "multiple-reset-" + UUID.randomUUID() + "@example.com";

        User user = createActiveCustomer(email, "OldPassword123!");

        assertEquals(202, performForgotPassword(email).getResponse().getStatus());
        assertEquals(202, performForgotPassword(email).getResponse().getStatus());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);

        verify(passwordResetEmailSender, times(2))
            .sendPasswordResetEmail(eq(email), tokenCaptor.capture());

        List<String> capturedTokens = tokenCaptor.getAllValues();

        assertEquals(2, capturedTokens.size());

        String firstToken = extractRawResetToken(capturedTokens.get(0));
        String secondToken = extractRawResetToken(capturedTokens.get(1));

        assertNotEquals(firstToken, secondToken);

        PasswordResetToken firstRecord = tokenRepository
            .findByTokenHash(secureTokenGenerator.hashToken(firstToken))
            .orElseThrow();

        PasswordResetToken secondRecord = tokenRepository
            .findByTokenHash(secureTokenGenerator.hashToken(secondToken))
            .orElseThrow();

        assertEquals(user.getId(), firstRecord.getUser().getId());
        assertEquals(user.getId(), secondRecord.getUser().getId());

        assertNotNull(firstRecord.getRevokedAt());
        assertNull(secondRecord.getRevokedAt());

        assertEquals(400, performResetPassword(firstToken, "NewPassword123!").getResponse().getStatus());
        assertEquals(204, performResetPassword(secondToken, "NewPassword123!").getResponse().getStatus());
    }

    private void expireResetToken(String rawToken) {
        transactionTemplate.executeWithoutResult(status -> {
            byte[] hash = secureTokenGenerator.hashToken(rawToken);
            PasswordResetToken token = tokenRepository.findByTokenHash(hash).orElseThrow();

            entityManager.createNativeQuery("""
                UPDATE password_reset_tokens
                SET created_at = :createdAt,
                    expires_at = :expiresAt
                WHERE id = :id
                """)
                .setParameter("createdAt", LocalDateTime.now(ZoneOffset.UTC).minusMinutes(20))
                .setParameter("expiresAt", LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5))
                .setParameter("id", token.getId())
                .executeUpdate();
        });
    }

    @Test
    void shouldRejectExpiredPasswordResetToken() throws Exception {
        String email = "expired-reset-" + UUID.randomUUID() + "@example.com";

        createActiveCustomer(email, "OldPassword123!");

        performForgotPassword(email);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetEmailSender).sendPasswordResetEmail(eq(email), tokenCaptor.capture());

        String rawToken = extractRawResetToken(tokenCaptor.getValue());

        expireResetToken(rawToken);

        MvcResult result = performResetPassword(rawToken, "NewPassword123!");

        assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    void shouldRejectUnknownResetToken() throws Exception {
        String invalidToken = secureTokenGenerator.generateToken();

        MvcResult result = performResetPassword(invalidToken, "NewPassword123!");

        assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    void shouldRejectPasswordResetWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                                "token": "some-token",
                                "newPassword": "NewPassword123!"
                            }
                            """))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectForgotPasswordWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                                "email": "customer@example.com"
                            }
                            """))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldInvalidateExistingSessionsAfterPasswordReset() throws Exception {
        String email = "reset-sessions-" + UUID.randomUUID() + "@example.com";
        String oldPassword = "OldPassword123!";
        String newPassword = "NewPassword123!";

        createActiveCustomer(email, oldPassword);

        // Establish an authenticated session before resetting the password.
        MvcResult loginResult = performLogin(email, oldPassword);

        assertEquals(200, loginResult.getResponse().getStatus());

        String oldAccessToken = extractAccessToken(loginResult);
        Cookie oldRefreshCookie = extractRefreshCookie(loginResult);

        // Request a password-reset email.
        performForgotPassword(email);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetEmailSender).sendPasswordResetEmail(eq(email), tokenCaptor.capture());

        String rawToken = extractRawResetToken(tokenCaptor.getValue());

        // Reset password.
        assertEquals(204, performResetPassword(rawToken, newPassword).getResponse().getStatus());

        // Previously issued JWT must no longer authenticate.
        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldAccessToken))
            .andExpect(status().isUnauthorized());

        // Previously issued refresh token must also fail.
        assertEquals(401, performRefresh(oldRefreshCookie).getResponse().getStatus());

        // Old password must no longer work.
        assertEquals(401, performLogin(email, oldPassword).getResponse().getStatus());

        // New password must work.
        assertEquals(200, performLogin(email, newPassword).getResponse().getStatus());
    }

    private String extractRawResetToken(String resetLink) {
        URI uri = URI.create(resetLink);
        String query = uri.getRawQuery();

        assertNotNull(query, "Password-reset link must contain a query string");

        return Arrays.stream(query.split("&"))
            .map(parameter -> parameter.split("=", 2))
            .filter(parts -> parts.length == 2 && parts[0].equals("token"))
            .map(parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Reset token is missing from the email link"));
    }

    @Test
    void shouldReturnStandardizedErrorWhenJwtIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.instance").value("/api/v1/auth/me"));
    }

    @Test
    void shouldReturnStandardizedErrorForInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturnStandardizedErrorWhenCsrfIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                                "email": "customer@example.com"
                            }
                            """))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"));
    }

    @Test
    void shouldReturnStandardizedErrorForInvalidCsrf() throws Exception {
        Cookie csrfCookie = obtainCsrfCookie();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", "incorrect-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                                "email": "customer@example.com"
                            }
                            """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"));
    }
}
