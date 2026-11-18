package com.ecommerce.identity.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    RSAPrivateKey jwtPrivateKey(@Value("${app.security.jwt.private-key}") Resource resource)
        throws Exception {
        String pem = resource.getContentAsString(StandardCharsets.UTF_8);

        String encoded =
            pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(encoded);

        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    @Bean
    RSAPublicKey jwtPublicKey(@Value("${app.security.jwt.public-key}") Resource resource)
        throws Exception {
        String pem = resource.getContentAsString(StandardCharsets.UTF_8);

        String encoded =
            pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(encoded);

        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    @Bean
    JwtEncoder jwtEncoder(
        RSAPublicKey publicKey, RSAPrivateKey privateKey, JwtProperties properties) {
        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(properties.keyId()).build();

        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean
    JwtDecoder jwtDecoder(RSAPublicKey publicKey, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(properties.issuer());

        OAuth2TokenValidator<Jwt> audienceValidator =
            jwt ->
                jwt.getAudience() != null && jwt.getAudience().contains(properties.audience())
                    ? OAuth2TokenValidatorResult.success()
                    : invalidToken("Invalid JWT audience");

        OAuth2TokenValidator<Jwt> tokenTypeValidator =
            jwt ->
                "access".equals(jwt.getClaimAsString("token_type"))
                    ? OAuth2TokenValidatorResult.success()
                    : invalidToken("Invalid JWT token type");

        OAuth2TokenValidator<Jwt> sessionIdValidator = jwt -> {
            try {
                String sessionId = jwt.getClaimAsString("sid");

                if (sessionId != null) {
                    UUID.fromString(sessionId);
                    return OAuth2TokenValidatorResult.success();
                }
            } catch (IllegalArgumentException exception) {
                // Invalid session UUID.
            }

            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Invalid JWT session identifier", null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerValidator, audienceValidator, tokenTypeValidator, sessionIdValidator));

        return decoder;
    }

    private static OAuth2TokenValidatorResult invalidToken(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
