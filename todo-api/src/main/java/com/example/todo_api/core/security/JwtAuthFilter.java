package com.example.todo_api.core.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Đọc "Authorization: Bearer ..." và gắn principal nếu token hợp lệ (AccessTokenAuthenticator).
 * Token không hợp lệ thì bỏ qua, endpoint cần đăng nhập sẽ trả 401.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AccessTokenAuthenticator authenticator;

    public JwtAuthFilter(AccessTokenAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            authenticator.authenticate(header.substring(7))
                    .ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));
        }
        chain.doFilter(request, response);
    }
}
