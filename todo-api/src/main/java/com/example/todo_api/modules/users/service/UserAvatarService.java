package com.example.todo_api.modules.users.service;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.infrastructure.IntegrationErrorCode;
import com.example.todo_api.infrastructure.r2.R2AvatarStorage;
import com.example.todo_api.modules.users.UserErrorCode;
import com.example.todo_api.modules.users.entity.User;
import com.example.todo_api.modules.users.repository.UserRepository;

@Service
public class UserAvatarService {

    private static final Map<String, String> EXT_BY_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    private final R2AvatarStorage storage;
    private final UserRepository userRepository;

    public UserAvatarService(R2AvatarStorage storage, UserRepository userRepository) {
        this.storage = storage;
        this.userRepository = userRepository;
    }

    public boolean uploadEnabled() {
        return storage.enabled();
    }

    @Transactional
    public User upload(User user, MultipartFile file) {
        if (!storage.enabled()) {
            throw new ApiException(IntegrationErrorCode.AVATAR_UPLOAD_DISABLED);
        }
        if (file == null || file.isEmpty()) {
            throw new ApiException(UserErrorCode.AVATAR_REQUIRED);
        }
        String contentType = normalizeContentType(file.getContentType());
        String extension = EXT_BY_TYPE.get(contentType);
        if (extension == null) {
            throw new ApiException(UserErrorCode.AVATAR_INVALID_TYPE);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(UserErrorCode.AVATAR_READ_FAILED);
        }
        String previousUrl = user.getAvatarUrl();
        String publicUrl = storage.upload(user.getId(), contentType, extension, bytes);
        try {
            user.changeAvatar(publicUrl);
        } catch (ApiException e) {
            storage.deleteIfOwned(publicUrl);
            throw e;
        }
        userRepository.save(user);
        storage.deleteIfOwned(previousUrl);
        return user;
    }

    private static String normalizeContentType(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }
}
