package com.example.todo_api.core.exceptions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;

import com.example.todo_api.core.web.TraceIdFilter;
import com.example.todo_api.modules.auth.AuthErrorCode;
import com.example.todo_api.modules.users.UserExceptionHandler;

class GlobalExceptionHandlerTest {

    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler(), new UserExceptionHandler())
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void apiException_isProblemDetailWithCodeAndTraceId() throws Exception {
        mvc.perform(get("/t/api-error").header(TraceIdFilter.HEADER, "trace-12345678"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(header().string(TraceIdFilter.HEADER, "trace-12345678"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Email is already registered"))
                .andExpect(jsonPath("$.instance").value("/t/api-error"))
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"))
                .andExpect(jsonPath("$.traceId").value("trace-12345678"));
    }

    @Test
    void beanValidation_listsFieldErrors() throws Exception {
        mvc.perform(post("/t/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    void malformedJson_isBadRequest() throws Exception {
        mvc.perform(post("/t/body").contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void wrongMethod_usesSameFormat() throws Exception {
        mvc.perform(delete("/t/api-error"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void unsupportedMediaType_usesSameFormat() throws Exception {
        mvc.perform(post("/t/body").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void constraintViolation_isValidationError() throws Exception {
        mvc.perform(get("/t/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    void dbUniqueViolation_isConflictNot500() throws Exception {
        mvc.perform(get("/t/db-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void accessDenied_isForbiddenNot500() throws Exception {
        mvc.perform(get("/t/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unexpectedError_hidesInternalsButKeepsTraceId() throws Exception {
        mvc.perform(get("/t/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value(CommonErrorCode.INTERNAL_ERROR.message()))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void uploadTooLarge_usesAvatarCode() throws Exception {
        mvc.perform(get("/t/too-large"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AVATAR_TOO_LARGE"));
    }

    record Body(@NotBlank String name) {
    }

    @RestController
    static class TestController {

        @GetMapping("/t/api-error")
        void apiError() {
            throw new ApiException(AuthErrorCode.EMAIL_EXISTS);
        }

        @PostMapping("/t/body")
        void body(@Valid @RequestBody Body body) {
        }

        @GetMapping("/t/constraint")
        void constraint() {
            try (var factory = Validation.buildDefaultValidatorFactory()) {
                Set<ConstraintViolation<Body>> violations = factory.getValidator().validate(new Body(""));
                throw new ConstraintViolationException(violations);
            }
        }

        @GetMapping("/t/db-conflict")
        void dbConflict() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint");
        }

        @GetMapping("/t/denied")
        void denied() {
            throw new AccessDeniedException("no");
        }

        @GetMapping("/t/boom")
        void boom() {
            throw new IllegalStateException("secret internal detail");
        }

        @GetMapping("/t/too-large")
        void tooLarge() {
            throw new MaxUploadSizeExceededException(2 * 1024 * 1024);
        }
    }
}
