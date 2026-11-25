package com.ecommerce.product.service;

import com.ecommerce.product.dto.request.AssignProductCategoriesRequest;
import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.request.UpdateProductRequest;
import com.ecommerce.product.dto.response.ProductResponse;

public interface ProductManagementService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse getProduct(Long productId);

    ProductResponse updateProduct(Long productId, UpdateProductRequest request);

    ProductResponse assignCategories(Long productId, AssignProductCategoriesRequest request);
}
