package com.example.todo_api.core.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

/**
 * Kiểm tra access token và dựng Authentication: chữ ký + hạn, blacklist (đăng xuất), tài khoản còn hoạt động,
 * token phát hành sau lần đổi mật khẩu gần nhất. Dùng chung cho HTTP (JwtAuthFilter) và WebSocket (STOMP CONNECT).
 */
@Component
public class AccessTokenAuthenticator {

    private final JwtVerifier jwtVerifier;
    private final AccessTokenBlacklist blacklist;
    private final AuthAccountLookup accountLookup;

    public AccessTokenAuthenticator(JwtVerifier jwtVerifier, AccessTokenBlacklist blacklist,
            AuthAccountLookup accountLookup) {
        this.jwtVerifier = jwtVerifier;
        this.blacklist = blacklist;
        this.accountLookup = accountLookup;
    }

    /** Rỗng nếu token không hợp lệ vì bất kỳ lý do nào. */
    public Optional<UsernamePasswordAuthenticationToken> authenticate(String token) {
        Claims claims;
        try {
            claims = jwtVerifier.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
        if (blacklist.contains(claims.getId())) {
            return Optional.empty();
        }
        AuthAccount account = accountLookup.findById(Long.valueOf(claims.getSubject())).orElse(null);
        if (account == null || !account.active()) {
            return Optional.empty();
        }
        // Đổi mật khẩu -> mọi access token phát hành trước đó mất hiệu lực (iat chỉ chính xác tới giây)
        Instant issuedAt = claims.getIssuedAt().toInstant();
        Instant changedAt = account.passwordChangedAt();
        if (changedAt != null && issuedAt.isBefore(changedAt.truncatedTo(ChronoUnit.SECONDS))) {
            return Optional.empty();
        }
        AuthPrincipal principal = new AuthPrincipal(account.id(), account.email(), account.role(), claims.getId(),
                claims.getExpiration().toInstant());
        return Optional.of(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + account.role()))));
    }
}
