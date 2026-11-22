package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.request.CreateBrandRequest;
import com.ecommerce.product.dto.request.UpdateBrandRequest;
import com.ecommerce.product.dto.response.BrandResponse;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.exception.BrandNotFoundException;
import com.ecommerce.product.exception.DuplicateBrandException;
import com.ecommerce.product.exception.InvalidBrandException;
import com.ecommerce.product.repository.BrandRepository;
import com.ecommerce.product.service.BrandManagementService;
import com.ecommerce.product.utils.SlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandManagementServiceImpl implements BrandManagementService {

    private static final int MAX_SLUG_LENGTH = 180;

    private final BrandRepository brandRepository;
    private final SlugGenerator slugGenerator;

    @Override
    @Transactional
    public BrandResponse createBrand(CreateBrandRequest request) {
        String name = validateName(request.name());

        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateBrandException(name);
        }

        Brand brand = Brand.builder()
            .name(name)
            .slug(generateUniqueSlug(name))
            .description(normalizeDescription(request.description()))
            .active(true)
            .build();

        return toResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(Long brandId, UpdateBrandRequest request) {
        Brand brand = brandRepository.findById(brandId)
            .orElseThrow(BrandNotFoundException::new);

        if (request.name() != null) {
            String name = validateName(request.name());

            if (!brand.getName().equalsIgnoreCase(name) && brandRepository.existsByNameIgnoreCase(name)) {
                throw new DuplicateBrandException(name);
            }

            brand.setName(name);
        }

        if (request.description() != null) {
            brand.setDescription(normalizeDescription(request.description()));
        }

        return toResponse(brand);
    }

    @Override
    @Transactional
    public BrandResponse setBrandActive(Long brandId, boolean active) {
        Brand brand = brandRepository.findById(brandId)
            .orElseThrow(BrandNotFoundException::new);

        if (!Boolean.valueOf(active).equals(brand.getActive())) {
            brand.setActive(active);
        }

        return toResponse(brand);
    }

    private String generateUniqueSlug(String name) {
        String normalized = slugGenerator.generate(name);

        if (normalized.isBlank()) {
            throw new InvalidBrandException("Brand name cannot produce a valid URL slug");
        }

        String baseSlug = normalized.substring(0, Math.min(normalized.length(), MAX_SLUG_LENGTH))
            .replaceAll("-+$", "");

        String candidate = baseSlug;
        int suffix = 2;

        while (brandRepository.existsBySlug(candidate)) {
            String suffixText = "-" + suffix++;
            int availableLength = MAX_SLUG_LENGTH - suffixText.length();

            candidate = baseSlug.substring(0, Math.min(baseSlug.length(), availableLength))
                .replaceAll("-+$", "") + suffixText;
        }

        return candidate;
    }

    private String validateName(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidBrandException("Brand name cannot be blank");
        }

        String name = value.strip();

        if (name.length() > 150) {
            throw new InvalidBrandException("Brand name cannot exceed 150 characters");
        }

        return name;
    }

    private String normalizeDescription(String value) {
        if (value == null) {
            return null;
        }

        String description = value.strip();

        if (description.length() > 1000) {
            throw new InvalidBrandException("Brand description cannot exceed 1000 characters");
        }

        return description;
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
            brand.getId(), brand.getName(), brand.getSlug(),
            brand.getDescription(), brand.getActive());
    }
}
