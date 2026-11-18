package com.ecommerce.identity.repository;

import com.ecommerce.identity.entity.EmailVerificationToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationToken, Long> {

  Optional<EmailVerificationToken> findByTokenHash(byte[] tokenHash);

  List<EmailVerificationToken> findByUser_IdAndConsumedAtIsNullAndRevokedAtIsNull(Long userId);

  Optional<EmailVerificationToken> findTopByUser_IdOrderByCreatedAtDesc(Long userId);

  long countByUser_IdAndCreatedAtGreaterThanEqual(Long userId, LocalDateTime createdAt);
}
