package com.ecommerce.identity.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@AttributeOverride(name = "userId", column = @Column(name = "user_id"))
@AttributeOverride(name = "roleId", column = @Column(name = "role_id"))
public class UserRoleId implements Serializable {

  private Long userId;

  private Long roleId;
}
