package com.ecommerce.identity.repository;

import com.ecommerce.identity.entity.UserStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStatusHistoryRepository extends JpaRepository<UserStatusHistory, Long> {

  List<UserStatusHistory> findByUser_IdOrderByChangedAtDesc(Long userId);
}
