package com.ecommerce.identity.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class SecureTokenGeneratorTest {

  private final SecureTokenGenerator tokenGenerator = new SecureTokenGenerator();

  @Test
  void shouldGenerate256BitRandomToken() {
    String token = tokenGenerator.generateToken();

    byte[] decoded = Base64.getUrlDecoder().decode(token);

    assertThat(decoded).hasSize(32);
  }

  @Test
  void shouldGenerateDifferentTokens() {
    String first = tokenGenerator.generateToken();
    String second = tokenGenerator.generateToken();

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  void shouldGenerateDeterministicSha256Hash() {
    String token = tokenGenerator.generateToken();

    byte[] firstHash = tokenGenerator.hashToken(token);
    byte[] secondHash = tokenGenerator.hashToken(token);

    assertThat(firstHash).hasSize(32);
    assertThat(firstHash).containsExactly(secondHash);
  }

  @Test
  void shouldGenerateDifferentHashesForDifferentTokens() {
    byte[] firstHash = tokenGenerator.hashToken(tokenGenerator.generateToken());
    byte[] secondHash = tokenGenerator.hashToken(tokenGenerator.generateToken());

    assertThat(Arrays.equals(firstHash, secondHash)).isFalse();
  }
}
