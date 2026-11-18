package com.ecommerce.identity.repository;

import com.ecommerce.identity.entity.PasswordResetToken;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(byte[] tokenHash);

    List<PasswordResetToken> findByUser_IdAndConsumedAtIsNullAndRevokedAtIsNull(Long userId);
}
