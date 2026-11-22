package com.ecommerce.product.service;

import com.ecommerce.product.dto.response.BrandResponse;
import java.util.List;

public interface BrandService {

    List<BrandResponse> getActiveBrands();

    BrandResponse getBrandBySlug(String slug);
}
