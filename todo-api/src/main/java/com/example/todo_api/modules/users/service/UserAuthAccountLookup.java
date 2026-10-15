package com.example.todo_api.modules.users.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.todo_api.core.security.AuthAccount;
import com.example.todo_api.core.security.AuthAccountLookup;
import com.example.todo_api.modules.users.repository.UserRepository;

/** Cung cấp thông tin tài khoản cho JwtAuthFilter (core) mà core không cần biết entity User. */
@Component
public class UserAuthAccountLookup implements AuthAccountLookup {

    private final UserRepository userRepository;

    public UserAuthAccountLookup(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<AuthAccount> findById(Long id) {
        return userRepository.findById(id)
                .map(user -> new AuthAccount(user.getId(), user.getEmail(), user.getRole().name(),
                        user.isActive(), user.getPasswordChangedAt()));
    }
}
