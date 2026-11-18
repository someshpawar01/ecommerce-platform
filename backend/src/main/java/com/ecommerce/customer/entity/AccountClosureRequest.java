package com.ecommerce.customer.entity;

import com.ecommerce.customer.enums.AccountClosureStatusEnum;
import com.ecommerce.identity.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "account_closure_requests")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountClosureRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 24)
  private AccountClosureStatusEnum status = AccountClosureStatusEnum.REQUESTED;

  @CreationTimestamp
  @Column(name = "requested_at", nullable = false, updatable = false)
  private LocalDateTime requestedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "resolution_note", length = 1000)
  private String resolutionNote;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_by_user_id")
  private User processedBy;
}
