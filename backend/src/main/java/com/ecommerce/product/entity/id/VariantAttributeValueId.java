package com.ecommerce.product.entity.id;

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
public class VariantAttributeValueId implements Serializable {

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "attribute_id")
    private Long attributeId;
}
