package com.example.todo_api.modules.auth.security;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;

import com.example.todo_api.config.AppProperties;
import com.example.todo_api.core.security.JwtKey;
import com.example.todo_api.modules.users.entity.User;

/** Phát access token (JWT) sau khi đăng nhập. Việc kiểm tra token nằm ở core (JwtVerifier). */
@Component
public class JwtIssuer {

    private final JwtKey key;
    private final AppProperties props;

    public JwtIssuer(JwtKey key, AppProperties props) {
        this.key = key;
        this.props = props;
    }

    public record AccessToken(String value, String jti, Instant expiresAt) {
    }

    public AccessToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(props.jwt().accessTtl());
        String jti = UUID.randomUUID().toString();
        String value = Jwts.builder()
                .subject(user.getId().toString())
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .signWith(key.get())
                .compact();
        return new AccessToken(value, jti, expiresAt);
    }
}
