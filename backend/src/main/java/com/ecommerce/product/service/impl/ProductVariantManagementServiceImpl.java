package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.request.CreateProductVariantRequest;
import com.ecommerce.product.dto.request.UpdateProductVariantRequest;
import com.ecommerce.product.dto.response.ProductVariantResponse;
import com.ecommerce.product.entity.*;
import com.ecommerce.product.entity.id.VariantAttributeValueId;
import com.ecommerce.product.enums.ProductStatusEnum;
import com.ecommerce.product.enums.ProductStatusEnum;
import com.ecommerce.product.exception.*;
import com.ecommerce.product.mapper.ProductVariantResponseMapper;
import com.ecommerce.product.repository.*;
import com.ecommerce.product.service.ProductVariantManagementService;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductVariantManagementServiceImpl implements ProductVariantManagementService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final VariantAttributeValueRepository variantAttributeValueRepository;
    private final ProductVariantResponseMapper responseMapper;

    @Override
    @Transactional
    public ProductVariantResponse createVariant(Long productId, CreateProductVariantRequest request) {
        Product product = productRepository.findByIdForUpdate(productId)
            .orElseThrow(ProductNotFoundException::new);

        requireDraft(product);

        String sku = validateSku(request.sku());
        String variantName = normalizeVariantName(request.variantName());

        validatePricing(request.price(), request.compareAtPrice());

        if (request.weightGrams() != null && request.weightGrams() <= 0) {
            throw new InvalidProductVariantException("Weight must be positive");
        }

        if (variantRepository.existsBySku(sku)) {
            throw new DuplicateProductVariantException("SKU already exists: " + sku);
        }

        List<AttributeValue> selectedValues = resolveAttributeValues(request.attributeValueIds());

        validateUniqueCombination(productId, selectedValues);

        ProductVariant variant = ProductVariant.builder()
            .product(product)
            .sku(sku)
            .variantName(variantName)
            .price(request.price())
            .compareAtPrice(request.compareAtPrice())
            .currencyCode("INR")
            .active(true)
            .weightGrams(request.weightGrams())
            .build();

        ProductVariant savedVariant = variantRepository.saveAndFlush(variant);

        List<VariantAttributeValue> assignments = selectedValues.stream()
            .map(value -> VariantAttributeValue.builder()
                .id(new VariantAttributeValueId(savedVariant.getId(), value.getAttribute().getId()))
                .variant(savedVariant)
                .attribute(value.getAttribute())
                .attributeValueId(value.getId())
                .attributeValue(value)
                .build())
            .toList();

        if (!assignments.isEmpty()) {
            variantAttributeValueRepository.saveAllAndFlush(assignments);
        }

        return responseMapper.toResponse(savedVariant, assignments);
    }

    private List<AttributeValue> resolveAttributeValues(List<Long> valueIds) {
        if (valueIds == null || valueIds.stream().anyMatch(Objects::isNull)) {
            throw new InvalidProductVariantException("Attribute value IDs cannot be null");
        }

        Set<Long> uniqueValueIds = new HashSet<>(valueIds);

        if (uniqueValueIds.size() != valueIds.size()) {
            throw new InvalidProductVariantException("Duplicate attribute value IDs are not allowed");
        }

        if (uniqueValueIds.isEmpty()) {
            return List.of();
        }

        List<AttributeValue> values = attributeValueRepository.findAllById(uniqueValueIds);

        if (values.size() != uniqueValueIds.size()) {
            throw new InvalidProductVariantException("One or more attribute values do not exist");
        }

        Set<Long> attributeIds = new HashSet<>();

        for (AttributeValue value : values) {
            Long attributeId = value.getAttribute().getId();

            if (!attributeIds.add(attributeId)) {
                throw new InvalidProductVariantException("Multiple values selected for the same attribute");
            }
        }

        return values;
    }

    private void validateUniqueCombination(Long productId, List<AttributeValue> selectedValues) {
        Set<Long> requestedIds = selectedValues.stream()
            .map(AttributeValue::getId)
            .collect(Collectors.toSet());

        List<ProductVariant> variants = variantRepository.findByProductIdOrderByIdAsc(productId);

        if (variants.isEmpty()) {
            return;
        }

        List<Long> variantIds = variants.stream()
            .map(ProductVariant::getId)
            .toList();

        List<VariantAttributeValue> assignments =
            variantAttributeValueRepository.findAllByVariantIdsWithDetails(variantIds);

        Map<Long, Set<Long>> combinations = new HashMap<>();

        for (VariantAttributeValue assignment : assignments) {
            combinations.computeIfAbsent(assignment.getVariant().getId(), ignored -> new HashSet<>())
                .add(assignment.getAttributeValue().getId());
        }

        for (ProductVariant variant : variants) {
            Set<Long> existingIds = combinations.getOrDefault(variant.getId(), Set.of());

            if (requestedIds.equals(existingIds)) {
                throw new DuplicateProductVariantException(
                    "A variant with the same attribute combination already exists");
            }
        }
    }

    private void requireDraft(Product product) {
        if (product.getStatus() != ProductStatusEnum.DRAFT) {
            throw new InvalidProductStateException("Variants can be modified only while the product is DRAFT");
        }
    }

    private String validateSku(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidProductVariantException("SKU cannot be blank");
        }

        String sku = value.strip();

        if (sku.length() > 100) {
            throw new InvalidProductVariantException("SKU cannot exceed 100 characters");
        }

        return sku;
    }

    private String normalizeVariantName(String value) {
        if (value == null) {
            return null;
        }

        String name = value.strip();

        if (name.length() > 255) {
            throw new InvalidProductVariantException("Variant name cannot exceed 255 characters");
        }

        return name;
    }

    private void validatePricing(BigDecimal price, BigDecimal compareAtPrice) {
        if (price == null || price.signum() < 0 || price.scale() > 2 || price.precision() - price.scale() > 10) {
            throw new InvalidProductVariantException("Invalid variant price");
        }

        if (compareAtPrice != null) {
            if (compareAtPrice.signum() < 0 || compareAtPrice.scale() > 2
                || compareAtPrice.precision() - compareAtPrice.scale() > 10) {
                throw new InvalidProductVariantException("Invalid compare-at price");
            }

            if (compareAtPrice.compareTo(price) < 0) {
                throw new InvalidProductVariantException("Compare-at price cannot be below selling price");
            }
        }
    }

    private List<VariantAttributeValue> getVariantAssignments(Long variantId) {
        return variantAttributeValueRepository.findAllByVariantIdsWithDetails(List.of(variantId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getProductVariants(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException();
        }

        List<ProductVariant> variants = variantRepository.findByProductIdOrderByIdAsc(productId);

        if (variants.isEmpty()) {
            return List.of();
        }

        List<Long> variantIds = variants.stream()
            .map(ProductVariant::getId)
            .toList();

        List<VariantAttributeValue> assignments =
            variantAttributeValueRepository.findAllByVariantIdsWithDetails(variantIds);

        Map<Long, List<VariantAttributeValue>> assignmentsByVariant = assignments.stream()
            .collect(Collectors.groupingBy(a -> a.getVariant().getId()));

        return variants.stream()
            .map(variant -> responseMapper.toResponse(
                variant, assignmentsByVariant.getOrDefault(variant.getId(), List.of())))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
            .orElseThrow(ProductVariantNotFoundException::new);

        return responseMapper.toResponse(variant, getVariantAssignments(variantId));
    }

    @Override
    @Transactional
    public ProductVariantResponse updateVariant(Long variantId, UpdateProductVariantRequest request) {
        ProductVariant variant = variantRepository.findById(variantId)
            .orElseThrow(ProductVariantNotFoundException::new);

        requireDraft(variant.getProduct());

        if (request.sku() != null) {
            String sku = validateSku(request.sku());

            if (!variant.getSku().equalsIgnoreCase(sku)
                && variantRepository.existsBySkuAndIdNot(sku, variantId)) {
                throw new DuplicateProductVariantException("SKU already exists: " + sku);
            }

            variant.setSku(sku);
        }

        if (request.variantName() != null) {
            variant.setVariantName(normalizeVariantName(request.variantName()));
        }

        BigDecimal price = request.price() != null ? request.price() : variant.getPrice();
        BigDecimal compareAtPrice = request.compareAtPrice() != null
            ? request.compareAtPrice() : variant.getCompareAtPrice();

        validatePricing(price, compareAtPrice);

        if (request.price() != null) {
            variant.setPrice(request.price());
        }

        if (request.compareAtPrice() != null) {
            variant.setCompareAtPrice(request.compareAtPrice());
        }

        if (request.weightGrams() != null) {
            if (request.weightGrams() <= 0) {
                throw new InvalidProductVariantException("Weight must be positive");
            }

            variant.setWeightGrams(request.weightGrams());
        }

        return responseMapper.toResponse(variant, getVariantAssignments(variantId));
    }

    @Override
    @Transactional
    public ProductVariantResponse setVariantActive(Long variantId, boolean active) {
        ProductVariant variant = variantRepository.findById(variantId)
            .orElseThrow(ProductVariantNotFoundException::new);

        requireDraft(variant.getProduct());

        if (!Boolean.valueOf(active).equals(variant.getActive())) {
            variant.setActive(active);
        }

        return responseMapper.toResponse(variant, getVariantAssignments(variantId));
    }

}
