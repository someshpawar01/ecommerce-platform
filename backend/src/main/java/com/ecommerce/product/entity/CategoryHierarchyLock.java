package com.ecommerce.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "category_hierarchy_lock")
@Getter
@Setter
public class CategoryHierarchyLock {

    @Id
    @Column(name = "id", nullable = false)
    private Byte id;
}
