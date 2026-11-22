package com.ecommerce.product.service;

import com.ecommerce.product.dto.request.CreateBrandRequest;
import com.ecommerce.product.dto.request.UpdateBrandRequest;
import com.ecommerce.product.dto.response.BrandResponse;

public interface BrandManagementService {

    BrandResponse createBrand(CreateBrandRequest request);

    BrandResponse updateBrand(Long brandId, UpdateBrandRequest request);

    BrandResponse setBrandActive(Long brandId, boolean active);
}
