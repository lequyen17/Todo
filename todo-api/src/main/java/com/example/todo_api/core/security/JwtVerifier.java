package com.example.todo_api.core.security;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/** Kiểm tra chữ ký + hạn của access token. Việc phát token nằm ở modules/auth (JwtIssuer). */
@Component
public class JwtVerifier {

    private final JwtKey key;

    public JwtVerifier(JwtKey key) {
        this.key = key;
    }

    /** Ném JwtException nếu chữ ký sai hoặc token đã hết hạn. */
    public Claims parse(String token) throws JwtException {
        return Jwts.parser().verifyWith(key.get()).build().parseSignedClaims(token).getPayload();
    }
}
