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
@AttributeOverride(name = "permissionId", column = @Column(name = "permission_id"))
@AttributeOverride(name = "roleId", column = @Column(name = "role_id"))
public class RolePermissionId implements Serializable {

  private Long roleId;

  private Long permissionId;
}
