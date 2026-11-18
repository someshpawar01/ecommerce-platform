package com.ecommerce.identity.repository;

import com.ecommerce.identity.entity.UserRole;
import com.ecommerce.identity.entity.UserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

  List<UserRole> findByUser_Id(Long userId);

  boolean existsByUser_IdAndRole_Code(Long userId, String roleCode);
}
