package com.ecommerce.identity.service.impl;

import com.ecommerce.identity.dto.response.CurrentUserResponse;
import com.ecommerce.identity.entity.User;
import com.ecommerce.identity.enums.UserStatusEnum;
import com.ecommerce.identity.repository.UserRepository;
import com.ecommerce.identity.service.CurrentUserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(Long userId) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AccessDeniedException("Access denied"));

        if (user.getStatus() != UserStatusEnum.ACTIVE) {
            throw new AccessDeniedException("Access denied");
        }

        List<String> roles = userRepository.findRoleCodesByUserId(userId);
        List<String> permissions = userRepository.findPermissionCodesByUserId(userId);

        return new CurrentUserResponse(
            user.getId(),
            user.getEmail(),
            List.copyOf(roles),
            List.copyOf(permissions)
        );
    }
}
