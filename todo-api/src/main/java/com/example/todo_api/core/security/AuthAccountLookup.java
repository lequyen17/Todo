package com.example.todo_api.core.security;

import java.util.Optional;

/**
 * core định nghĩa, module quản lý user cài đặt (UserAuthAccountLookup).
 * Nhờ vậy core/security không phụ thuộc vào entity hay repository của modules.
 */
public interface AuthAccountLookup {

    Optional<AuthAccount> findById(Long id);
}
