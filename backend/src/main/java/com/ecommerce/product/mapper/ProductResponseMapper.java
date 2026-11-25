package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.response.ProductCategoryResponse;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductCategory;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductResponseMapper {

    public ProductResponse toResponse(Product product, List<ProductCategory> categories) {
        Brand brand = product.getBrand();

        List<ProductCategoryResponse> categoryResponses = categories.stream()
            .map(pc -> new ProductCategoryResponse(
                pc.getCategory().getId(),
                pc.getCategory().getName(),
                pc.getCategory().getSlug(),
                pc.getPrimary()))
            .toList();

        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getSlug(),
            brand == null ? null : brand.getId(),
            brand == null ? null : brand.getName(),
            product.getShortDescription(),
            product.getDescription(),
            product.getStatus(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            product.getPublishedAt(),
            product.getVersion(),
            categoryResponses
        );
    }
}
