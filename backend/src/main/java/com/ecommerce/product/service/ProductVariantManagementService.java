package com.ecommerce.product.service;

import com.ecommerce.product.dto.request.CreateProductVariantRequest;
import com.ecommerce.product.dto.request.UpdateProductVariantRequest;
import com.ecommerce.product.dto.response.ProductVariantResponse;
import java.util.List;

public interface ProductVariantManagementService {

    ProductVariantResponse createVariant(Long productId, CreateProductVariantRequest request);

    List<ProductVariantResponse> getProductVariants(Long productId);

    ProductVariantResponse getVariant(Long variantId);

    ProductVariantResponse updateVariant(Long variantId, UpdateProductVariantRequest request);

    ProductVariantResponse setVariantActive(Long variantId, boolean active);
}
