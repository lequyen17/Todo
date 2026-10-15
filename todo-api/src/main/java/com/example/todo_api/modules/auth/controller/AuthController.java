package com.example.todo_api.modules.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.core.security.AuthPrincipal;
import com.example.todo_api.core.security.RefreshTokenCookieService;
import com.example.todo_api.core.utils.ClientIp;
import com.example.todo_api.modules.auth.dto.AuthSession;
import com.example.todo_api.modules.auth.dto.ChallengeRequest;
import com.example.todo_api.modules.auth.dto.ChangePasswordRequest;
import com.example.todo_api.modules.auth.dto.EmailRequest;
import com.example.todo_api.modules.auth.dto.GoogleConfigResponse;
import com.example.todo_api.modules.auth.dto.GoogleLoginRequest;
import com.example.todo_api.modules.auth.dto.LoginResponse;
import com.example.todo_api.modules.auth.dto.LoginRequest;
import com.example.todo_api.modules.auth.dto.LoginResult;
import com.example.todo_api.modules.auth.dto.MessageResponse;
import com.example.todo_api.modules.auth.dto.RegisterRequest;
import com.example.todo_api.modules.auth.dto.ResetPasswordRequest;
import com.example.todo_api.modules.auth.dto.TokenResponse;
import com.example.todo_api.modules.auth.dto.VerifyLoginRequest;
import com.example.todo_api.modules.auth.dto.VerifyRegisterRequest;
import com.example.todo_api.modules.auth.service.GoogleAuthService;
import com.example.todo_api.modules.auth.service.LoginService;
import com.example.todo_api.modules.auth.service.PasswordService;
import com.example.todo_api.modules.auth.service.RegistrationService;
import com.example.todo_api.modules.auth.service.SessionService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;
    private final GoogleAuthService googleAuthService;
    private final SessionService sessionService;
    private final PasswordService passwordService;
    private final RefreshTokenCookieService refreshCookie;

    public AuthController(RegistrationService registrationService, LoginService loginService,
            GoogleAuthService googleAuthService, SessionService sessionService, PasswordService passwordService,
            RefreshTokenCookieService refreshCookie) {
        this.registrationService = registrationService;
        this.loginService = loginService;
        this.googleAuthService = googleAuthService;
        this.sessionService = sessionService;
        this.passwordService = passwordService;
        this.refreshCookie = refreshCookie;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    MessageResponse register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        registrationService.register(req, ClientIp.of(http));
        return new MessageResponse("A verification code has been sent to your email");
    }

    @PostMapping("/register/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    MessageResponse resendRegister(@Valid @RequestBody EmailRequest req, HttpServletRequest http) {
        registrationService.resendOtp(req.email(), ClientIp.of(http));
        return new MessageResponse("A new verification code has been sent");
    }

    @PostMapping("/register/verify")
    @ResponseStatus(HttpStatus.CREATED)
    TokenResponse verifyRegister(@Valid @RequestBody VerifyRegisterRequest req, HttpServletRequest http,
            HttpServletResponse response) {
        return attachRefreshCookie(response, registrationService.verify(req.email(), req.otp(), ClientIp.of(http)));
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http, HttpServletResponse response) {
        LoginResult result = loginService.login(req, ClientIp.of(http));
        if (result.refreshTokenPlain() != null) {
            refreshCookie.write(response, result.refreshTokenPlain());
        }
        return result.body();
    }

    @PostMapping("/login/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    MessageResponse resendLogin(@Valid @RequestBody ChallengeRequest req, HttpServletRequest http) {
        loginService.resendOtp(req.challengeId(), ClientIp.of(http));
        return new MessageResponse("A new verification code has been sent");
    }

    @PostMapping("/login/verify")
    TokenResponse verifyLogin(@Valid @RequestBody VerifyLoginRequest req, HttpServletRequest http,
            HttpServletResponse response) {
        return attachRefreshCookie(response, loginService.verifyOtp(req.challengeId(), req.otp(), ClientIp.of(http)));
    }

    @GetMapping("/google/config")
    GoogleConfigResponse googleConfig() {
        return googleAuthService.config();
    }

    @PostMapping("/google")
    TokenResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest req, HttpServletRequest http,
            HttpServletResponse response) {
        return attachRefreshCookie(response, googleAuthService.login(req.credential(), ClientIp.of(http)));
    }

    @PostMapping("/refresh")
    TokenResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshCookie.read(request).orElse(null);
        if (refreshToken == null) {
            refreshCookie.clear(response);
            throw invalidRefreshToken();
        }
        try {
            return attachRefreshCookie(response, sessionService.refresh(refreshToken));
        } catch (ApiException e) {
            if ("INVALID_REFRESH_TOKEN".equals(e.getCode())) {
                refreshCookie.clear(response);
            }
            throw e;
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@AuthenticationPrincipal AuthPrincipal principal, HttpServletRequest request,
            HttpServletResponse response) {
        refreshCookie.read(request).ifPresent(token -> sessionService.logout(principal, token));
        refreshCookie.clear(response);
    }

    @PostMapping("/change-password")
    TokenResponse changePassword(@AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest req, HttpServletResponse response) {
        return attachRefreshCookie(response, passwordService.change(principal, req));
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    MessageResponse forgotPassword(@Valid @RequestBody EmailRequest req, HttpServletRequest http) {
        passwordService.forgot(req.email(), ClientIp.of(http));
        return new MessageResponse("If the email exists, a password reset link has been sent");
    }

    @PostMapping("/reset-password")
    MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req, HttpServletRequest http) {
        passwordService.reset(req, ClientIp.of(http));
        return new MessageResponse("Password has been reset, please sign in again");
    }

    private TokenResponse attachRefreshCookie(HttpServletResponse response, AuthSession session) {
        refreshCookie.write(response, session.refreshTokenPlain());
        return session.tokens();
    }

    private static ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                "Session has expired, please sign in again");
    }
}
