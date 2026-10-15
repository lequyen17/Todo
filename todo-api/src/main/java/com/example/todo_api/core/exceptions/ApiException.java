package com.example.todo_api.core.exceptions;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final ErrorCode code;

    /** args điền vào các %s/%d trong message của mã lỗi. */
    public ApiException(ErrorCode code, Object... args) {
        super(args.length == 0 ? code.message() : code.message().formatted(args));
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return code.status();
    }
}
