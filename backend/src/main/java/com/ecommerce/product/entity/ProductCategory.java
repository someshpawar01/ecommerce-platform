package com.ecommerce.product.entity;

import com.ecommerce.product.entity.id.ProductCategoryId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ProductCategory {

    @EmbeddedId
    private ProductCategoryId id;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @MapsId("categoryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "is_primary", nullable = false)
    private Boolean primary = false;

    @Column(name = "primary_product_id", insertable = false, updatable = false)
    private Long primaryProductId;
}
