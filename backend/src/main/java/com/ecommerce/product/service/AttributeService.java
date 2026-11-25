package com.ecommerce.product.service;

import com.ecommerce.product.dto.response.AttributeResponse;
import com.ecommerce.product.dto.response.AttributeValueResponse;
import java.util.List;

public interface AttributeService {

    List<AttributeResponse> getAttributes();

    AttributeResponse getAttribute(Long attributeId);

    List<AttributeValueResponse> getAttributeValues(Long attributeId);
}
