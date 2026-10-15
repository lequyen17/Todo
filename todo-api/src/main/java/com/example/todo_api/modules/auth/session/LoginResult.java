package com.example.todo_api.modules.auth.session;

import com.example.todo_api.modules.auth.dto.LoginChallengeResponse;
import com.example.todo_api.modules.auth.dto.LoginResponse;

/** Kết quả /login: body trả cho client + refresh token gốc (null khi còn chờ OTP). Không trả trực tiếp ra JSON. */
public record LoginResult(LoginResponse body, String refreshTokenPlain) {

    public static LoginResult challenge(LoginChallengeResponse challenge) {
        return new LoginResult(LoginResponse.challenge(challenge), null);
    }

    public static LoginResult authenticated(AuthSession session) {
        return new LoginResult(LoginResponse.authenticated(session.tokens()), session.refreshTokenPlain());
    }
}
