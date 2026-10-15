package com.example.todo_api.core.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Mọi lỗi trả về cùng một định dạng ProblemDetail (xem ApiProblem).
 * Kế thừa ResponseEntityExceptionHandler để các lỗi của Spring MVC (404, 405, 415, sai kiểu tham số...)
 * cũng theo định dạng này thay vì trang lỗi mặc định của Spring Boot.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApi(ApiException e) {
        return ApiProblem.response(e.getCode(), e.getMessage(), Map.of());
    }

    /** Ném từ entry point của Spring Security (SecurityConfig) hoặc từ controller. */
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> handleAuthentication(AuthenticationException e) {
        return ApiProblem.response(CommonErrorCode.UNAUTHORIZED);
    }

    /** Phải khai báo riêng, nếu không lỗi phân quyền từ @PreAuthorize sẽ rơi vào handler 500 bên dưới. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> handleAccessDenied(AccessDeniedException e) {
        return ApiProblem.response(CommonErrorCode.FORBIDDEN);
    }

    /** Validate tham số ở service/controller có @Validated. */
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> {
            String path = v.getPropertyPath().toString();
            fields.putIfAbsent(path.substring(path.lastIndexOf('.') + 1), v.getMessage());
        });
        return ApiProblem.response(CommonErrorCode.VALIDATION_ERROR, CommonErrorCode.VALIDATION_ERROR.message(),
                fields);
    }

    /** Vi phạm ràng buộc DB (vd. hai request đăng ký cùng email chạy song song vượt qua bước kiểm tra trước). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return ApiProblem.response(CommonErrorCode.CONFLICT);
    }

    /** Lưới an toàn cuối: ghi log đầy đủ, client chỉ nhận thông báo chung + traceId để báo lỗi. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return ApiProblem.response(CommonErrorCode.INTERNAL_ERROR);
    }

    // ---------------------------------------------------------------- Ghi đè lỗi của Spring MVC

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(err -> fields.putIfAbsent(err.getField(), err.getDefaultMessage()));
        return ApiProblem.response(CommonErrorCode.VALIDATION_ERROR, CommonErrorCode.VALIDATION_ERROR.message(),
                fields);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ApiProblem.response(CommonErrorCode.BAD_REQUEST);
    }

    /** Các lỗi MVC còn lại (404, 405, 415, thiếu tham số...): giữ ProblemDetail của Spring, thêm code + traceId. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        if (body instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey("code"))) {
            ApiProblem.decorate(problem, ApiProblem.codeFor(status));
        }
        return super.createResponseEntity(body, headers, status, request);
    }
}
