package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.request.CreateAttributeRequest;
import com.ecommerce.product.dto.request.CreateAttributeValueRequest;
import com.ecommerce.product.dto.request.UpdateAttributeRequest;
import com.ecommerce.product.dto.request.UpdateAttributeValueRequest;
import com.ecommerce.product.dto.response.AttributeResponse;
import com.ecommerce.product.dto.response.AttributeValueResponse;
import com.ecommerce.product.entity.Attribute;
import com.ecommerce.product.entity.AttributeValue;
import com.ecommerce.product.exception.AttributeNotFoundException;
import com.ecommerce.product.exception.AttributeValueInUseException;
import com.ecommerce.product.exception.AttributeValueNotFoundException;
import com.ecommerce.product.exception.DuplicateAttributeException;
import com.ecommerce.product.exception.DuplicateAttributeValueException;
import com.ecommerce.product.exception.InvalidAttributeException;
import com.ecommerce.product.repository.AttributeRepository;
import com.ecommerce.product.repository.AttributeValueRepository;
import com.ecommerce.product.service.AttributeManagementService;
import com.ecommerce.product.utils.SlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttributeManagementServiceImpl implements AttributeManagementService {

    private static final int MAX_CODE_LENGTH = 80;

    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final SlugGenerator slugGenerator;

    @Override
    @Transactional
    public AttributeResponse createAttribute(CreateAttributeRequest request) {
        String name = validateName(request.name());
        String code = generateCode(name);

        if (attributeRepository.existsByCode(code)) {
            throw new DuplicateAttributeException(code);
        }

        Attribute attribute = Attribute.builder()
            .code(code)
            .name(name)
            .build();

        return toAttributeResponse(attributeRepository.save(attribute));
    }

    @Override
    @Transactional
    public AttributeResponse updateAttribute(Long attributeId, UpdateAttributeRequest request) {
        Attribute attribute = attributeRepository.findById(attributeId)
            .orElseThrow(AttributeNotFoundException::new);

        String name = validateName(request.name());

        if (!attribute.getName().equals(name)) {
            attribute.setName(name);
        }

        return toAttributeResponse(attribute);
    }

    @Override
    @Transactional
    public AttributeValueResponse createAttributeValue(Long attributeId, CreateAttributeValueRequest request) {
        Attribute attribute = attributeRepository.findById(attributeId)
            .orElseThrow(AttributeNotFoundException::new);

        String value = validateValue(request.value());

        if (attributeValueRepository.existsByAttributeIdAndValueIgnoreCase(attributeId, value)) {
            throw new DuplicateAttributeValueException(value);
        }

        AttributeValue attributeValue = AttributeValue.builder()
            .attribute(attribute)
            .value(value)
            .displayOrder(0)
            .build();

        return toAttributeValueResponse(attributeValueRepository.save(attributeValue));
    }

    @Override
    @Transactional
    public AttributeValueResponse updateAttributeValue(Long valueId, UpdateAttributeValueRequest request) {
        AttributeValue attributeValue = attributeValueRepository.findById(valueId)
            .orElseThrow(AttributeValueNotFoundException::new);

        String value = validateValue(request.value());

        if (attributeValue.getValue().equals(value)) {
            return toAttributeValueResponse(attributeValue);
        }

        if (attributeValueRepository.countVariantReferences(valueId) > 0) {
            throw new AttributeValueInUseException();
        }

        Long attributeId = attributeValue.getAttribute().getId();

        if (attributeValueRepository.existsByAttributeIdAndValueIgnoreCaseAndIdNot(attributeId, value, valueId)) {
            throw new DuplicateAttributeValueException(value);
        }

        attributeValue.setValue(value);

        return toAttributeValueResponse(attributeValue);
    }

    private String generateCode(String name) {
        String code = slugGenerator.generate(name);

        if (code.isBlank()) {
            throw new InvalidAttributeException("Attribute name cannot produce a valid code");
        }

        return code.substring(0, Math.min(code.length(), MAX_CODE_LENGTH))
            .replaceAll("-+$", "");
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidAttributeException("Attribute name cannot be blank");
        }

        String normalized = name.strip();

        if (normalized.length() > 100) {
            throw new InvalidAttributeException("Attribute name cannot exceed 100 characters");
        }

        return normalized;
    }

    private String validateValue(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidAttributeException("Attribute value cannot be blank");
        }

        String normalized = value.strip();

        if (normalized.length() > 150) {
            throw new InvalidAttributeException("Attribute value cannot exceed 150 characters");
        }

        return normalized;
    }

    private AttributeResponse toAttributeResponse(Attribute attribute) {
        return new AttributeResponse(attribute.getId(), attribute.getCode(), attribute.getName());
    }

    private AttributeValueResponse toAttributeValueResponse(AttributeValue value) {
        return new AttributeValueResponse(
            value.getId(), value.getAttribute().getId(), value.getValue(), value.getDisplayOrder());
    }
}
