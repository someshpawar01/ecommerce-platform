package com.ecommerce.identity.repository;

import com.ecommerce.identity.dto.projection.LoginCredentialsProjection;
import com.ecommerce.identity.entity.User;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailNormalized(String emailNormalized);

    boolean existsByEmailNormalized(String emailNormalized);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.emailNormalized = :email")
    Optional<User> findByEmailNormalizedWithLock(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    @Query(value = """
        SELECT DISTINCT r.code
        FROM user_roles ur
        JOIN roles r ON r.id = ur.role_id
        WHERE ur.user_id = :userId
        ORDER BY r.code
        """, nativeQuery = true)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    @Query(value = """
        SELECT DISTINCT p.code
        FROM user_roles ur
        JOIN role_permissions rp ON rp.role_id = ur.role_id
        JOIN permissions p ON p.id = rp.permission_id
        WHERE ur.user_id = :userId
        ORDER BY p.code
        """, nativeQuery = true)
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    //Result must be mapped to ProjectionEntity ie.why the query is like this.
    @Query("""
        SELECT new com.ecommerce.identity.dto.projection.LoginCredentialsProjection(
            u.id, u.passwordHash
        )
        FROM User u
        WHERE u.emailNormalized = :email
        """)
    Optional<LoginCredentialsProjection> findLoginCredentials(@Param("email") String email);
}
