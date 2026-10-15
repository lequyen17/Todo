package com.example.todo_api.modules.auth.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.modules.users.entity.User;
import com.example.todo_api.modules.users.entity.UserStatus;

/** Quy tắc dùng chung cho các luồng xác thực. */
final class AuthRules {

    private AuthRules() {
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static void ensureActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", "Account is locked");
        }
    }
}
