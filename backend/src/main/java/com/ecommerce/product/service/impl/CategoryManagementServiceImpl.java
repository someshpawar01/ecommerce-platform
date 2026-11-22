package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.request.CreateCategoryRequest;
import com.ecommerce.product.dto.response.CategoryResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.exception.CategoryNotFoundException;
import com.ecommerce.product.exception.InvalidCategoryException;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.service.CategoryManagementService;
import com.ecommerce.product.utils.CategorySlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryManagementServiceImpl implements CategoryManagementService {

    private static final int MAX_SLUG_LENGTH = 180;

    private final CategoryRepository categoryRepository;
    private final CategorySlugGenerator slugGenerator;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String name = requireNonBlank(request.name(), "Category name");

        Category parent = request.parentId() == null ? null : categoryRepository.findById(request.parentId())
            .orElseThrow(CategoryNotFoundException::new);

        String slug = generateUniqueSlug(name);
        int sortOrder = determineNextSortOrder(request.parentId());

        Category category = Category.builder()
            .name(name)
            .slug(slug)
            .description(normalizeNullable(request.description()))
            .parent(parent)
            .sortOrder(sortOrder)
            .active(true)
            .build();

        return toResponse(categoryRepository.save(category));
    }

    private String generateUniqueSlug(String name) {
        String normalizedSlug = slugGenerator.generate(name);

        if (normalizedSlug.isBlank()) {
            throw new InvalidCategoryException("Category name cannot produce a valid URL slug");
        }

        String baseSlug = normalizedSlug.substring(0, Math.min(normalizedSlug.length(), MAX_SLUG_LENGTH))
            .replaceAll("-+$", "");

        String candidate = baseSlug;
        int suffix = 2;

        while (categoryRepository.existsBySlug(candidate)) {
            String suffixText = "-" + suffix++;
            int availableLength = MAX_SLUG_LENGTH - suffixText.length();

            candidate = baseSlug.substring(0, Math.min(baseSlug.length(), availableLength))
                .replaceAll("-+$", "") + suffixText;
        }

        return candidate;
    }

    private int determineNextSortOrder(Long parentId) {
        Integer maxSortOrder = parentId == null
            ? categoryRepository.findMaxRootSortOrder()
            : categoryRepository.findMaxChildSortOrder(parentId);

        return Math.addExact(maxSortOrder, 1);
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidCategoryException(fieldName + " cannot be blank");
        }

        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null ? null : value.trim();
    }

    private CategoryResponse toResponse(Category category) {
        Long parentId = category.getParent() == null ? null : category.getParent().getId();

        return new CategoryResponse(
            category.getId(),
            parentId,
            category.getName(),
            category.getSlug(),
            category.getDescription(),
            category.getSortOrder()
        );
    }
}
