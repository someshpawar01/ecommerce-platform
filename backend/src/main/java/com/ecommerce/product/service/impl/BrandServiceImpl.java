package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.response.BrandResponse;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.exception.BrandNotFoundException;
import com.ecommerce.product.repository.BrandRepository;
import com.ecommerce.product.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    @Override
    public List<BrandResponse> getActiveBrands() {
        return brandRepository.findByActiveTrueOrderByNameAscIdAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public BrandResponse getBrandBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug)
            .filter(b -> Boolean.TRUE.equals(b.getActive()))
            .orElseThrow(BrandNotFoundException::new);

        return toResponse(brand);
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
            brand.getId(), brand.getName(), brand.getSlug(),
            brand.getDescription(), brand.getActive());
    }
}
