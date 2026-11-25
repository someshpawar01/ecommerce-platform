package com.ecommerce.product.service;

import com.ecommerce.product.dto.request.CreateAttributeRequest;
import com.ecommerce.product.dto.request.CreateAttributeValueRequest;
import com.ecommerce.product.dto.request.UpdateAttributeRequest;
import com.ecommerce.product.dto.request.UpdateAttributeValueRequest;
import com.ecommerce.product.dto.response.AttributeResponse;
import com.ecommerce.product.dto.response.AttributeValueResponse;

public interface AttributeManagementService {

    AttributeResponse createAttribute(CreateAttributeRequest request);

    AttributeResponse updateAttribute(Long attributeId, UpdateAttributeRequest request);

    AttributeValueResponse createAttributeValue(Long attributeId, CreateAttributeValueRequest request);

    AttributeValueResponse updateAttributeValue(Long valueId, UpdateAttributeValueRequest request);
}
