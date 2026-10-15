package com.example.todo_api.modules.users.controller;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.todo_api.core.security.AuthPrincipal;
import com.example.todo_api.modules.users.dto.UserProfileResponse;
import com.example.todo_api.modules.users.service.UserAvatarService;
import com.example.todo_api.modules.users.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserAvatarService avatarService;

    public UserController(UserService userService, UserAvatarService avatarService) {
        this.userService = userService;
        this.avatarService = avatarService;
    }

    @GetMapping("/me")
    UserProfileResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return UserProfileResponse.from(userService.getCurrent(principal.userId()), avatarService.uploadEnabled());
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    UserProfileResponse uploadAvatar(@AuthenticationPrincipal AuthPrincipal principal,
            @RequestPart("file") MultipartFile file) {
        return UserProfileResponse.from(avatarService.upload(userService.getCurrent(principal.userId()), file), avatarService.uploadEnabled());
    }
}
