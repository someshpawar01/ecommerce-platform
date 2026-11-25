package com.ecommerce.product.policy;

import com.ecommerce.product.entity.Category;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CategoryVisibilityPolicyImpl implements CategoryVisibilityPolicy {

    @Override
    public boolean isVisible(Category category) {
        if (category == null) {
            return false;
        }

        Set<Long> visited = new HashSet<>();
        Category current = category;

        while (current != null) {
            if (!visited.add(current.getId())) {
                return false;
            }

            if (!Boolean.TRUE.equals(current.getActive())) {
                return false;
            }

            current = current.getParent();
        }

        return true;
    }
}
