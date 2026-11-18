package com.ecommerce.identity.security;

import com.ecommerce.identity.config.JwtProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties properties;
  private final Clock clock;

  public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
    this.jwtEncoder = jwtEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  public String generateAccessToken(
      Long userId, UUID sessionId, List<String> roles, List<String> permissions) {
    Instant now = clock.instant();

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(userId.toString())
            .audience(List.of(properties.audience()))
            .issuedAt(now)
            .notBefore(now)
            .expiresAt(now.plus(properties.accessTokenMinutes(), ChronoUnit.MINUTES))
            .id(UUID.randomUUID().toString())
            .claim("token_type", "access")
            .claim("sid", sessionId.toString())
            .claim("roles", List.copyOf(roles))
            .claim("permissions", List.copyOf(permissions))
            .build();

    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(properties.keyId()).build();

    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
