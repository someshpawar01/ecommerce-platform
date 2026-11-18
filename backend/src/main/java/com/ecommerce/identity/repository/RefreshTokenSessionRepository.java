package com.ecommerce.identity.repository;

import com.ecommerce.identity.entity.RefreshTokenSession;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, Long> {

    Optional<RefreshTokenSession> findByTokenHash(byte[] tokenHash);

    boolean existsBySessionFamilyIdAndUser_IdAndRevokedAtIsNullAndExpiresAtAfterAndAbsoluteExpiresAtAfter(
        UUID sessionFamilyId, Long userId, LocalDateTime now, LocalDateTime absoluteNow);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM RefreshTokenSession s WHERE s.tokenHash = :tokenHash")
    Optional<RefreshTokenSession> findByTokenHashForUpdate(@Param("tokenHash") byte[] tokenHash);

    //notice how user.id is written
    @Query("""
        SELECT s.user.id
        FROM RefreshTokenSession s
        WHERE s.tokenHash = :tokenHash
        """)
    Optional<Long> findUserIdByTokenHash(@Param("tokenHash") byte[] tokenHash);

    //JPQL needs @Modyfying when we are doing update or delete using @Query
    @Modifying
    @Query("""
        UPDATE RefreshTokenSession s
        SET s.revokedAt = :now,
            s.revokeReason = :reason
        WHERE s.sessionFamilyId = :sessionFamilyId
          AND s.revokedAt IS NULL
        """)
    int revokeSessionFamily(@Param("sessionFamilyId") UUID sessionFamilyId,
                            @Param("now") LocalDateTime now,
                            @Param("reason") String reason);

    @Modifying
    @Query("""
        UPDATE RefreshTokenSession s
        SET s.revokedAt = :now,
            s.revokeReason = :reason
        WHERE s.revokedAt IS NULL AND s.user.id= :userId
        """)
    int revokeAllUserSessions(@Param("userId") Long userId,
                            @Param("now") LocalDateTime now,
                            @Param("reason") String reason);
}
