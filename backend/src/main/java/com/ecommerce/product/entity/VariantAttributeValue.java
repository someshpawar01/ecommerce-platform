package com.ecommerce.product.entity;

import com.ecommerce.product.entity.id.VariantAttributeValueId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "variant_attribute_values")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VariantAttributeValue {

    @EmbeddedId
    private VariantAttributeValueId id;

    @MapsId("variantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @MapsId("attributeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;

    @Column(name = "attribute_value_id", nullable = false)
    private Long attributeValueId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "attribute_id", referencedColumnName = "attribute_id",
            insertable = false, updatable = false),
        @JoinColumn(name = "attribute_value_id", referencedColumnName = "id",
            insertable = false, updatable = false)
    })
    private AttributeValue attributeValue;

    public VariantAttributeValue(ProductVariant variant, AttributeValue attributeValue) {
        this.variant = variant;
        this.attribute = attributeValue.getAttribute();
        this.attributeValue = attributeValue;
        this.attributeValueId = attributeValue.getId();
        this.id = new VariantAttributeValueId(variant.getId(), attribute.getId());
    }
}
