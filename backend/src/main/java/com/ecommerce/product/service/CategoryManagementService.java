package com.ecommerce.product.service;

import com.ecommerce.product.dto.request.CreateCategoryRequest;
import com.ecommerce.product.dto.request.UpdateCategoryRequest;
import com.ecommerce.product.dto.response.CategoryResponse;

public interface CategoryManagementService {

    CategoryResponse createCategory(CreateCategoryRequest request);
    CategoryResponse updateCategory(Long categoryId, UpdateCategoryRequest request);

    CategoryResponse setCategoryActive(Long categoryId, boolean active);
}
