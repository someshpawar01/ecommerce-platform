package com.ecommerce.product.service;

import com.ecommerce.product.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getRootCategories();

    CategoryResponse getCategoryBySlug(String slug);

    List<CategoryResponse> getChildCategories(String slug);
}
