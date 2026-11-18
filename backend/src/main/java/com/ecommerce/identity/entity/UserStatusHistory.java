package com.ecommerce.identity.entity;

import com.ecommerce.identity.enums.UserStatusEnum;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user_status_history")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserStatusHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "old_status", length = 24)
  private UserStatusEnum oldStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false, length = 24)
  private UserStatusEnum newStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "changed_by_user_id")
  private User changedBy;

  @Column(name = "reason", length = 500)
  private String reason;

  @Column(name = "correlation_id", length = 100)
  private String correlationId;

  @CreationTimestamp
  @Column(name = "changed_at", nullable = false, updatable = false)
  private LocalDateTime changedAt;
}
