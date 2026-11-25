package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.response.AttributeResponse;
import com.ecommerce.product.dto.response.AttributeValueResponse;
import com.ecommerce.product.entity.Attribute;
import com.ecommerce.product.entity.AttributeValue;
import com.ecommerce.product.exception.AttributeNotFoundException;
import com.ecommerce.product.repository.AttributeRepository;
import com.ecommerce.product.repository.AttributeValueRepository;
import com.ecommerce.product.service.AttributeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttributeServiceImpl implements AttributeService {

    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;

    @Override
    public List<AttributeResponse> getAttributes() {
        return attributeRepository.findAllByOrderByNameAscIdAsc().stream()
            .map(this::toAttributeResponse)
            .toList();
    }

    @Override
    public AttributeResponse getAttribute(Long attributeId) {
        Attribute attribute = attributeRepository.findById(attributeId)
            .orElseThrow(AttributeNotFoundException::new);

        return toAttributeResponse(attribute);
    }

    @Override
    public List<AttributeValueResponse> getAttributeValues(Long attributeId) {
        if (!attributeRepository.existsById(attributeId)) {
            throw new AttributeNotFoundException();
        }

        return attributeValueRepository.findByAttributeIdOrderByDisplayOrderAscValueAscIdAsc(attributeId)
            .stream()
            .map(this::toAttributeValueResponse)
            .toList();
    }

    private AttributeResponse toAttributeResponse(Attribute attribute) {
        return new AttributeResponse(attribute.getId(), attribute.getCode(), attribute.getName());
    }

    private AttributeValueResponse toAttributeValueResponse(AttributeValue value) {
        return new AttributeValueResponse(
            value.getId(), value.getAttribute().getId(), value.getValue(), value.getDisplayOrder());
    }
}
