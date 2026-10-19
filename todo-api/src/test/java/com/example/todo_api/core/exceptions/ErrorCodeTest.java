package com.example.todo_api.core.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.example.todo_api.infrastructure.IntegrationErrorCode;
import com.example.todo_api.modules.auth.AuthErrorCode;
import com.example.todo_api.modules.notifications.NotificationErrorCode;
import com.example.todo_api.modules.settings.SettingsErrorCode;
import com.example.todo_api.modules.tasks.TaskErrorCode;
import com.example.todo_api.modules.users.UserErrorCode;

class ErrorCodeTest {

    /** Frontend chỉ thấy chuỗi code, nên hai enum không được dùng trùng tên. */
    @Test
    void codeNamesAreUniqueAcrossModules() {
        List<String> names = new ArrayList<>();
        Stream.of(CommonErrorCode.values(), IntegrationErrorCode.values(), AuthErrorCode.values(),
                UserErrorCode.values(), TaskErrorCode.values(), SettingsErrorCode.values(),
                NotificationErrorCode.values())
                .flatMap(Arrays::stream)
                .forEach(code -> names.add(code.name()));

        assertThat(names).doesNotHaveDuplicates();
    }

    @Test
    void messageArgumentsAreFilledIn() {
        ApiException e = new ApiException(AuthErrorCode.OTP_INVALID, 2L);

        assertThat(e.getMessage()).isEqualTo("Incorrect verification code, 2 attempt(s) left");
        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(e.getCode().name()).isEqualTo("OTP_INVALID");
    }

    @Test
    void plainCodeUsesDefaultMessage() {
        ApiException e = new ApiException(AuthErrorCode.EMAIL_EXISTS);

        assertThat(e.getMessage()).isEqualTo("Email is already registered");
        assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }
}
