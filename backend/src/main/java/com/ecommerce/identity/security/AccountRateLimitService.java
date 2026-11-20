package com.ecommerce.identity.security;

import com.ecommerce.common.ratelimit.RedisRateLimiter;
import com.ecommerce.identity.exception.ratelimit.AccountRateLimitExceededException;
import com.ecommerce.identity.exception.ratelimit.RateLimitUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class AccountRateLimitService {
    //Instead of creating multi key lua script which will involve additional complexity
    // we are following this approach.

    //Also doing this rate limiting inside RateLimitingFilter will cause filter to read HTTP
    // request body and generally HTTP request bodies are consumed when read
    //If the filter consumes the input stream, our controller might not be able to deserialize
    // the request body without additional wrapping and caching

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RedisRateLimiter redisRateLimiter;
    private final byte[] hmacKey;

    public AccountRateLimitService(RedisRateLimiter redisRateLimiter,
                                   @Value("${app.ratelimit.identity-hmac-key}") String hmacKey) {
        this.redisRateLimiter = redisRateLimiter;

        if (hmacKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("Rate-limit HMAC key must contain at least 32 UTF-8 bytes");
        }

        this.hmacKey = hmacKey.getBytes(StandardCharsets.UTF_8);
    }

    public void checkLogin(String normalizedEmail) {
        check("login", normalizedEmail, 5, 60);
    }

    public void checkForgotPassword(String normalizedEmail) {
        check("forgot-password", normalizedEmail, 3, 900);
    }

    public void checkResendVerification(String normalizedEmail) {
        check("resend-verification", normalizedEmail, 3, 900);
    }

    private void check(String policyName, String normalizedEmail, int limit, long windowSeconds) {
        String emailHash = hashIdentifier(normalizedEmail);
        String redisKey = "ecommerce:ratelimit:" + policyName + ":email:" + emailHash;

        try {
            boolean allowed = redisRateLimiter.allow(redisKey, limit, windowSeconds);

            if (!allowed) {
                throw new AccountRateLimitExceededException();
            }
        } catch (DataAccessException exception) {
            throw new RateLimitUnavailableException(exception);
        }
    }

    private String hashIdentifier(String identifier) {
        try {
            //Using HMAC_SHA instead of SHA256 because it allows some secret to be added
            //Preventing attacker from cheaply precompute the SHA256 for email.
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hmacKey, HMAC_ALGORITHM));

            byte[] digest = mac.doFinal(identifier.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to calculate rate-limit identifier", exception);
        }
    }
}
