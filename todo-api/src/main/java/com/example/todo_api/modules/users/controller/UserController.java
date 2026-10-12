package com.example.todo_api.modules.users.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.todo_api.core.security.AuthPrincipal;
import com.example.todo_api.modules.users.dto.UserProfileResponse;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    UserProfileResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return UserProfileResponse.from(principal.user());
    }
}
