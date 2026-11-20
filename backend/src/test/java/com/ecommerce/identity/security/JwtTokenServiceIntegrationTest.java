package com.ecommerce.identity.security;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.identity.config.JwtProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ecommerce.identity.service.VerificationEmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class JwtTokenServiceIntegrationTest {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private VerificationEmailSender verificationEmailSender;

    @MockitoBean
    private AccountRateLimitService accountRateLimitService;

    @Test
    void shouldGenerateAndValidateAccessToken() {
        Long userId = 101L;
        UUID sessionFamilyId = UUID.randomUUID();

        List<String> roles = List.of("CUSTOMER");
        List<String> permissions = List.of("ORDER_READ", "PROFILE_READ");

        String accessToken = jwtTokenService.generateAccessToken(
            userId, sessionFamilyId, roles, permissions);

        Jwt jwt = jwtDecoder.decode(accessToken);

        assertNotNull(accessToken);
        assertEquals(userId.toString(), jwt.getSubject());
        assertEquals(sessionFamilyId.toString(), jwt.getClaimAsString("sid"));

        assertEquals(jwtProperties.issuer(), jwt.getClaimAsString("iss"));
        assertEquals(List.of(jwtProperties.audience()), jwt.getAudience());

        assertEquals("access", jwt.getClaimAsString("token_type"));
        assertEquals(roles, jwt.getClaimAsStringList("roles"));
        assertEquals(permissions, jwt.getClaimAsStringList("permissions"));

        assertNotNull(jwt.getId());
        assertNotNull(jwt.getIssuedAt());
        assertNotNull(jwt.getExpiresAt());
    }

    @Test
    void shouldGenerateTokenWithConfiguredExpiration() {
        String accessToken = jwtTokenService.generateAccessToken(
            101L, UUID.randomUUID(), List.of("CUSTOMER"), List.of("ORDER_READ"));

        Jwt jwt = jwtDecoder.decode(accessToken);

        long expirationSeconds = Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toSeconds();

        assertEquals(jwtProperties.accessTokenMinutes() * 60, expirationSeconds);
    }

    @Test
    void shouldRejectTokenWithModifiedSignature() {
        String accessToken = jwtTokenService.generateAccessToken(
            101L, UUID.randomUUID(), List.of("CUSTOMER"), List.of("ORDER_READ"));

        String[] parts = accessToken.split("\\.");

        assertEquals(3, parts.length);

        char firstCharacter = parts[2].charAt(0);
        char replacement = firstCharacter == 'A' ? 'B' : 'A';

        parts[2] = replacement + parts[2].substring(1);

        String tamperedToken = String.join(".", parts);

        assertThrows(JwtException.class, () -> jwtDecoder.decode(tamperedToken));
    }

    @Test
    void shouldGenerateUniqueTokenIdentifiers() {
        Long userId = 101L;
        UUID sessionFamilyId = UUID.randomUUID();

        String firstToken = jwtTokenService.generateAccessToken(
            userId, sessionFamilyId, List.of("CUSTOMER"), List.of("ORDER_READ"));

        String secondToken = jwtTokenService.generateAccessToken(
            userId, sessionFamilyId, List.of("CUSTOMER"), List.of("ORDER_READ"));

        Jwt firstJwt = jwtDecoder.decode(firstToken);
        Jwt secondJwt = jwtDecoder.decode(secondToken);

        assertNotEquals(firstJwt.getId(), secondJwt.getId());
    }

    private String signTestToken(String issuer, String audience, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(List.of(audience))
            .subject("101")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .id(UUID.randomUUID().toString())
            .claim("token_type", "access")
            .claim("sid", UUID.randomUUID().toString())
            .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
            .keyId(jwtProperties.keyId())
            .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Test
    void shouldRejectExpiredJwt() {
        Instant now = Instant.now();

        String token = signTestToken(
            jwtProperties.issuer(),
            jwtProperties.audience(),
            now.minusSeconds(600),
            now.minusSeconds(300));

        assertThrows(JwtException.class, () -> jwtDecoder.decode(token));
    }

    @Test
    void shouldRejectJwtWithInvalidIssuer() {
        Instant now = Instant.now();

        String token = signTestToken(
            "untrusted-issuer",
            jwtProperties.audience(),
            now,
            now.plusSeconds(900));

        assertThrows(JwtException.class, () -> jwtDecoder.decode(token));
    }

    @Test
    void shouldRejectJwtWithInvalidAudience() {
        Instant now = Instant.now();

        String token = signTestToken(
            jwtProperties.issuer(),
            "some-other-api",
            now,
            now.plusSeconds(900));

        assertThrows(JwtException.class, () -> jwtDecoder.decode(token));
    }
}
