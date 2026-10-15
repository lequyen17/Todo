package com.example.todo_api.modules.users.service;

import org.springframework.stereotype.Service;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.core.exceptions.CommonErrorCode;
import com.example.todo_api.modules.users.entity.User;
import com.example.todo_api.modules.users.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** User của principal hiện tại; không còn trong DB thì coi như phiên không hợp lệ. */
    public User getCurrent(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(CommonErrorCode.UNAUTHORIZED));
    }
}
