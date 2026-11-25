package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.response.CategoryResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.exception.CategoryNotFoundException;
import com.ecommerce.product.policy.CategoryVisibilityPolicy;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.service.CategoryService;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private final CategoryVisibilityPolicy categoryVisibilityPolicy;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentIsNullAndActiveTrueOrderBySortOrderAscNameAscIdAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
            .orElseThrow(CategoryNotFoundException::new);

        if (!categoryVisibilityPolicy.isVisible(category)) {
            throw new CategoryNotFoundException();
        }

        return toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildCategories(String slug) {
        Category parent = categoryRepository.findBySlug(slug)
            .orElseThrow(CategoryNotFoundException::new);

        if (!categoryVisibilityPolicy.isVisible(parent)) {
            throw new CategoryNotFoundException();
        }

        return categoryRepository.findByParentIdAndActiveTrueOrderBySortOrderAscNameAscIdAsc(parent.getId())
            .stream()
            .map(this::toResponse)
            .toList();
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
