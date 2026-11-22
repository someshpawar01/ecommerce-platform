package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.request.CreateCategoryRequest;
import com.ecommerce.product.dto.request.ReorderCategoriesRequest;
import com.ecommerce.product.dto.request.UpdateCategoryRequest;
import com.ecommerce.product.dto.response.CategoryResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.exception.CategoryNotFoundException;
import com.ecommerce.product.exception.InvalidCategoryException;
import com.ecommerce.product.exception.InvalidCategoryHierarchyException;
import com.ecommerce.product.exception.InvalidCategoryOrderException;
import com.ecommerce.product.repository.CategoryHierarchyLockRepository;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.service.CategoryManagementService;
import com.ecommerce.product.utils.SlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryManagementServiceImpl implements CategoryManagementService {

    private static final int MAX_SLUG_LENGTH = 180;

    private final CategoryRepository categoryRepository;
    private final SlugGenerator slugGenerator;
    private final CategoryHierarchyLockRepository hierarchyLockRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        lockHierarchy();
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

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(CategoryNotFoundException::new);

        if (request.name() != null) {
            String name = requireNonBlank(request.name(), "Category name");

            if (name.length() > 150) {
                throw new InvalidCategoryException("Category name cannot exceed 150 characters");
            }

            category.setName(name);
        }

        if (request.description() != null) {
            String description = request.description().trim();

            if (description.length() > 1000) {
                throw new InvalidCategoryException("Category description cannot exceed 1000 characters");
            }

            category.setDescription(description);
        }

        return toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse setCategoryActive(Long categoryId, boolean active) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(CategoryNotFoundException::new);

        if (Boolean.valueOf(active).equals(category.getActive())) {
            return toResponse(category);
        }

        category.setActive(active);

        return toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse changeParent(Long categoryId, Long newParentId) {
        lockHierarchy();

        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(CategoryNotFoundException::new);

        Long currentParentId = category.getParent() == null ? null : category.getParent().getId();

        if (Objects.equals(currentParentId, newParentId)) {
            return toResponse(category);
        }

        Category newParent = null;

        if (newParentId != null) {
            newParent = categoryRepository.findById(newParentId)
                .orElseThrow(CategoryNotFoundException::new);

            validateNoCycle(category, newParent);
        }

        category.setParent(newParent);
        category.setSortOrder(determineNextSortOrder(newParentId));

        return toResponse(category);
    }

    @Override
    @Transactional
    public List<CategoryResponse> reorderCategories(ReorderCategoriesRequest request) {
        lockHierarchy();

        if (request.categoryIds() == null) {
            throw new InvalidCategoryOrderException("Category IDs are required");
        }

        Long parentId = request.parentId();

        if (parentId != null && !categoryRepository.existsById(parentId)) {
            throw new CategoryNotFoundException();
        }

        List<Category> siblings = parentId == null ? categoryRepository.findAllRootCategoriesForOrdering()
            : categoryRepository.findAllChildrenForOrdering(parentId);

        List<Long> requestedIds = request.categoryIds();

        if (requestedIds.stream().anyMatch(Objects::isNull)) {
            throw new InvalidCategoryOrderException("Category IDs cannot contain null values");
        }

        Set<Long> uniqueIds = new HashSet<>(requestedIds);

        if (uniqueIds.size() != requestedIds.size()) {
            throw new InvalidCategoryOrderException("Category IDs must not contain duplicates");
        }

        Set<Long> existingIds = siblings.stream()
            .map(Category::getId)
            .collect(Collectors.toSet());

        if (requestedIds.size() != siblings.size() || !uniqueIds.equals(existingIds)) {
            throw new InvalidCategoryOrderException(
                "Provided category IDs do not match the parent's immediate children");
        }

        Map<Long, Category> categoriesById = siblings.stream()
            .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<CategoryResponse> responses = new ArrayList<>(requestedIds.size());

        for (int sortOrder = 0; sortOrder < requestedIds.size(); sortOrder++) {
            Category category = categoriesById.get(requestedIds.get(sortOrder));
            int newSortOrder = sortOrder + 1;

            if (!Objects.equals(category.getSortOrder(), newSortOrder)) {
                category.setSortOrder(newSortOrder);
            }

            responses.add(toResponse(category));
        }

        return responses;
    }

    private void lockHierarchy() {
        hierarchyLockRepository.findByIdForUpdate((byte) 1)
            .orElseThrow(() -> new IllegalStateException("Category hierarchy lock row is missing"));
    }

    private void validateNoCycle(Category category, Category proposedParent) {
        Set<Long> visited = new HashSet<>();
        Category current = proposedParent;

        while (current != null) {
            if (!visited.add(current.getId())) {
                throw new InvalidCategoryHierarchyException("Existing category hierarchy contains a cycle");
            }

            if (current.getId().equals(category.getId())) {
                throw new InvalidCategoryHierarchyException("A category cannot be moved under itself or its descendants");
            }

            current = current.getParent();
        }
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
            category.getSortOrder(),
            category.getActive()
        );
    }
}
