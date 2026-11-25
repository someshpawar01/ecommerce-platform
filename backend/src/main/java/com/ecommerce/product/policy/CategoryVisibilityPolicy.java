package com.ecommerce.product.policy;

import com.ecommerce.product.entity.Category;

public interface CategoryVisibilityPolicy {

    boolean isVisible(Category category);
}
