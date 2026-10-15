package com.example.todo_api.core.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.security.Keys;

import com.example.todo_api.config.AppProperties;

/** Khoá HMAC dùng chung cho việc ký (modules/auth) và kiểm tra (core) access token. */
@Component
public class JwtKey {

    private final SecretKey key;

    public JwtKey(AppProperties props) {
        String secret = props.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public SecretKey get() {
        return key;
    }
}
