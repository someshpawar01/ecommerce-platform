package com.ecommerce.identity.service.impl;

import com.ecommerce.identity.api.CustomerIdentityReader;
import com.ecommerce.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerIdentityReaderImpl implements CustomerIdentityReader {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public String getEmail(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("Identity record missing for customer"))
            .getEmail();
    }
}
