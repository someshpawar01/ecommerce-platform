package com.ecommerce.customer.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "customer_addresses")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerAddress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_user_id", nullable = false)
  private CustomerProfile customerProfile;

  @Column(name = "label", length = 40)
  private String label;

  @Column(name = "recipient_name", nullable = false, length = 150)
  private String recipientName;

  @Column(name = "recipient_phone_e164", nullable = false, length = 16)
  private String recipientPhoneE164;

  @Column(name = "line1", nullable = false, length = 255)
  private String line1;

  @Column(name = "line2", length = 255)
  private String line2;

  @Column(name = "landmark", length = 150)
  private String landmark;

  @Column(name = "locality", length = 150)
  private String locality;

  @Column(name = "city", nullable = false, length = 100)
  private String city;

  @Column(name = "state", nullable = false, length = 100)
  private String state;

  @Column(name = "postal_code", nullable = false, length = 20)
  private String postalCode;

  @Column(name = "country_code", nullable = false, length = 2)
  private String countryCode = "IN";

  @Column(name = "is_default", nullable = false)
  private Boolean isDefault = false;

  @Column(name = "default_customer_user_id", insertable = false, updatable = false)
  private Long defaultCustomerUserId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
