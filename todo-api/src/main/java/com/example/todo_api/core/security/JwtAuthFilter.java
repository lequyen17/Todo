package com.example.todo_api.core.security;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Đọc "Authorization: Bearer ..." và gắn principal nếu token hợp lệ.
 * Token không hợp lệ thì bỏ qua, endpoint cần đăng nhập sẽ trả 401.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtVerifier jwtVerifier;
    private final AccessTokenBlacklist blacklist;
    private final AuthAccountLookup accountLookup;

    public JwtAuthFilter(JwtVerifier jwtVerifier, AccessTokenBlacklist blacklist, AuthAccountLookup accountLookup) {
        this.jwtVerifier = jwtVerifier;
        this.blacklist = blacklist;
        this.accountLookup = accountLookup;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            authenticate(header.substring(7));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        Claims claims;
        try {
            claims = jwtVerifier.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            return;
        }
        if (blacklist.contains(claims.getId())) {
            return;
        }
        AuthAccount account = accountLookup.findById(Long.valueOf(claims.getSubject())).orElse(null);
        if (account == null || !account.active()) {
            return;
        }
        // Đổi mật khẩu -> mọi access token phát hành trước đó mất hiệu lực (iat chỉ chính xác tới giây)
        Instant issuedAt = claims.getIssuedAt().toInstant();
        Instant changedAt = account.passwordChangedAt();
        if (changedAt != null && issuedAt.isBefore(changedAt.truncatedTo(ChronoUnit.SECONDS))) {
            return;
        }
        AuthPrincipal principal = new AuthPrincipal(account.id(), account.email(), account.role(), claims.getId(),
                claims.getExpiration().toInstant());
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + account.role())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
