package com.example.todo_api.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import io.jsonwebtoken.Jwts;

import com.example.todo_api.config.AppProperties;

class JwtAuthFilterTest {

    private JwtKey key;
    private AccessTokenBlacklist blacklist;
    private AuthAccountLookup lookup;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(null, null,
                new AppProperties.Jwt("0123456789abcdef0123456789abcdef", Duration.ofMinutes(15),
                        Duration.ofDays(7), Duration.ofDays(30)),
                null, null, null, new AppProperties.Google(null),
                new AppProperties.R2(null, null, null, null, null));
        key = new JwtKey(props);
        blacklist = mock(AccessTokenBlacklist.class);
        lookup = mock(AuthAccountLookup.class);
        filter = new JwtAuthFilter(new JwtVerifier(key), blacklist, lookup);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_setsPrincipalWithPlainData() throws Exception {
        when(lookup.findById(7L)).thenReturn(Optional.of(account(true, null)));

        Authentication auth = run(token("jti-1", Instant.now()));

        assertThat(auth).isNotNull();
        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo(7L);
        assertThat(principal.jti()).isEqualTo("jti-1");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
    }

    @Test
    void lockedAccount_isNotAuthenticated() throws Exception {
        when(lookup.findById(7L)).thenReturn(Optional.of(account(false, null)));

        assertThat(run(token("jti-1", Instant.now()))).isNull();
    }

    @Test
    void blacklistedToken_isNotAuthenticated() throws Exception {
        when(lookup.findById(7L)).thenReturn(Optional.of(account(true, null)));
        when(blacklist.contains("jti-1")).thenReturn(true);

        assertThat(run(token("jti-1", Instant.now()))).isNull();
    }

    @Test
    void tokenIssuedBeforePasswordChange_isNotAuthenticated() throws Exception {
        Instant issuedAt = Instant.now().minus(Duration.ofMinutes(5));
        when(lookup.findById(7L)).thenReturn(Optional.of(account(true, Instant.now())));

        assertThat(run(token("jti-1", issuedAt))).isNull();
    }

    @Test
    void tamperedToken_isNotAuthenticated() throws Exception {
        assertThat(run(token("jti-1", Instant.now()) + "x")).isNull();
    }

    private Authentication run(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private String token(String jti, Instant issuedAt) {
        Instant iat = issuedAt.truncatedTo(ChronoUnit.SECONDS);
        return Jwts.builder()
                .subject("7")
                .id(jti)
                .issuedAt(Date.from(iat))
                .expiration(Date.from(iat.plus(Duration.ofMinutes(15))))
                .signWith(key.get())
                .compact();
    }

    private static AuthAccount account(boolean active, Instant passwordChangedAt) {
        return new AuthAccount(7L, "a@example.com", "USER", active, passwordChangedAt);
    }
}
