package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.request.AssignProductCategoriesRequest;
import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.request.UpdateProductRequest;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductCategory;
import com.ecommerce.product.entity.id.ProductCategoryId;
import com.ecommerce.product.enums.ProductStatusEnum;
import com.ecommerce.product.exception.*;
import com.ecommerce.product.mapper.ProductResponseMapper;
import com.ecommerce.product.repository.BrandRepository;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductCategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.service.ProductManagementService;
import com.ecommerce.product.policy.CategoryVisibilityPolicy;
import com.ecommerce.product.utils.SlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductManagementServiceImpl implements ProductManagementService {

    private static final int MAX_SLUG_LENGTH = 300;

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final SlugGenerator slugGenerator;
    private final ProductResponseMapper responseMapper;
    private final CategoryVisibilityPolicy categoryVisibilityPolicy;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        String name = validateName(request.name());
        Brand brand = resolveBrand(request.brandId());

        Product product = Product.builder()
            .name(name)
            .slug(generateUniqueSlug(name))
            .brand(brand)
            .shortDescription(normalizeShortDescription(request.shortDescription()))
            .description(request.description())
            .status(ProductStatusEnum.DRAFT)
            .build();

        Product savedProduct = productRepository.save(product);

        return responseMapper.toResponse(savedProduct, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(ProductNotFoundException::new);

        List<ProductCategory> categories = productCategoryRepository.findAllByProductIdWithCategory(productId);

        return responseMapper.toResponse(product, categories);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
            .orElseThrow(ProductNotFoundException::new);

        requireDraft(product);

        if (request.name() != null) {
            product.setName(validateName(request.name()));
        }

        if (request.brandId() != null) {
            product.setBrand(resolveBrand(request.brandId()));
        }

        if (request.shortDescription() != null) {
            product.setShortDescription(normalizeShortDescription(request.shortDescription()));
        }

        if (request.description() != null) {
            product.setDescription(request.description());
        }

        List<ProductCategory> categories = productCategoryRepository.findAllByProductIdWithCategory(productId);

        return responseMapper.toResponse(product, categories);
    }

    @Override
    @Transactional
    public ProductResponse assignCategories(Long productId, AssignProductCategoriesRequest request) {
        Product product = productRepository.findByIdForUpdate(productId)
            .orElseThrow(ProductNotFoundException::new);

        requireDraft(product);

        if (request.categoryIds() == null || request.categoryIds().stream().anyMatch(Objects::isNull)) {
            throw new InvalidProductCategoryException("Category IDs cannot be null");
        }

        Set<Long> uniqueIds = new HashSet<>(request.categoryIds());

        if (uniqueIds.size() != request.categoryIds().size()) {
            throw new InvalidProductCategoryException("Duplicate category IDs are not allowed");
        }

        if (uniqueIds.isEmpty() != (request.primaryCategoryId() == null)) {
            throw new InvalidProductCategoryException("Primary category must be provided exactly when categories exist");
        }

        if (!uniqueIds.isEmpty() && !uniqueIds.contains(request.primaryCategoryId())) {
            throw new InvalidProductCategoryException("Primary category must be included in categoryIds");
        }

        List<Category> categories = uniqueIds.isEmpty() ? List.of() : categoryRepository.findAllById(uniqueIds);

        if (categories.size() != uniqueIds.size()) {
            throw new InvalidProductCategoryException("One or more categories do not exist");
        }

        for (Category category : categories) {
            if (!categoryVisibilityPolicy.isVisible(category)) {
                throw new InvalidProductCategoryException("Inactive categories cannot be assigned to products");
            }
        }

        List<ProductCategory> assignments = categories.stream()
            .map(category -> ProductCategory.builder()
                .id(new ProductCategoryId(productId, category.getId()))
                .product(product)
                .category(category)
                .primary(category.getId().equals(request.primaryCategoryId()))
                .build())
            .toList();

        productCategoryRepository.deleteAllByProductId(productId);

        if (!assignments.isEmpty()) {
            productCategoryRepository.saveAllAndFlush(assignments);
        }

        List<ProductCategory> savedAssignments = productCategoryRepository.findAllByProductIdWithCategory(productId);

        return responseMapper.toResponse(product, savedAssignments);
    }

    private void requireDraft(Product product) {
        if (product.getStatus() != ProductStatusEnum.DRAFT) {
            throw new InvalidProductStateException("Only draft products can be modified");
        }
    }

    private Brand resolveBrand(Long brandId) {
        if (brandId == null) {
            return null;
        }

        Brand brand = brandRepository.findById(brandId)
            .orElseThrow(BrandNotFoundException::new);

        if (!Boolean.TRUE.equals(brand.getActive())) {
            throw new InvalidProductException("An inactive brand cannot be assigned to a product");
        }

        return brand;
    }

    private String validateName(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidProductException("Product name cannot be blank");
        }

        String name = value.strip();

        if (name.length() > 255) {
            throw new InvalidProductException("Product name cannot exceed 255 characters");
        }

        return name;
    }

    private String normalizeShortDescription(String value) {
        if (value == null) {
            return null;
        }

        String description = value.strip();

        if (description.length() > 500) {
            throw new InvalidProductException("Short description cannot exceed 500 characters");
        }

        return description;
    }

    private String generateUniqueSlug(String name) {
        String normalized = slugGenerator.generate(name);

        if (normalized.isBlank()) {
            throw new InvalidProductException("Product name cannot produce a valid URL slug");
        }

        String baseSlug = normalized.substring(0, Math.min(normalized.length(), MAX_SLUG_LENGTH))
            .replaceAll("-+$", "");

        String candidate = baseSlug;
        int suffix = 2;

        while (productRepository.existsBySlug(candidate)) {
            String suffixText = "-" + suffix++;
            int availableLength = MAX_SLUG_LENGTH - suffixText.length();

            candidate = baseSlug.substring(0, Math.min(baseSlug.length(), availableLength))
                .replaceAll("-+$", "") + suffixText;
        }

        return candidate;
    }
}
