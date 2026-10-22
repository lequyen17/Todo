package com.example.todo_api.core.exceptions;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;



/**
 * Body lỗi chuẩn RFC 9457 (application/problem+json):
 * {type, title, status, detail, instance, code, traceId, fields?}.
 * code = mã máy đọc (ErrorCode); traceId = mã tra log; fields = lỗi theo từng trường của form.
 */
public final class ApiProblem {

    private ApiProblem() {
    }

    public static ResponseEntity<Object> response(ErrorCode code) {
        return response(code, code.message(), Map.of());
    }

    public static ResponseEntity<Object> response(ErrorCode code, String detail, Map<String, String> fields) {
        return ResponseEntity.status(code.status()).body(of(code.status(), code.name(), detail, fields));
    }

    public static ProblemDetail of(HttpStatusCode status, String code, String detail, Map<String, String> fields) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        decorate(problem, code);
        if (!fields.isEmpty()) {
            problem.setProperty("fields", fields);
        }
        return problem;
    }

    /** Thêm code + traceId; dùng cho cả ProblemDetail do Spring tự tạo (404, 405, 415...). */
    public static void decorate(ProblemDetail problem, String code) {
        problem.setProperty("code", code);
// traceId handling removed
    }

    /** Lỗi do framework sinh ra không có ErrorCode riêng: dùng tên HTTP status (NOT_FOUND, METHOD_NOT_ALLOWED...). */
    public static String codeFor(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.name() : "ERROR";
    }
}
