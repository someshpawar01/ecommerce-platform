package com.ecommerce.product.service;

import com.ecommerce.product.dto.request.CreateCategoryRequest;
import com.ecommerce.product.dto.response.CategoryResponse;

public interface CategoryManagementService {

    CategoryResponse createCategory(CreateCategoryRequest request);
}
