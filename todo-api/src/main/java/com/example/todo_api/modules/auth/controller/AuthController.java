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
import jakarta.validation.Valid;

import com.example.todo_api.core.security.AuthPrincipal;
import com.example.todo_api.core.utils.ClientIp;
import com.example.todo_api.modules.auth.dto.ChallengeRequest;
import com.example.todo_api.modules.auth.dto.ChangePasswordRequest;
import com.example.todo_api.modules.auth.dto.EmailRequest;
import com.example.todo_api.modules.auth.dto.GoogleConfigResponse;
import com.example.todo_api.modules.auth.dto.GoogleLoginRequest;
import com.example.todo_api.modules.auth.dto.LoginResponse;
import com.example.todo_api.modules.auth.dto.LoginRequest;
import com.example.todo_api.modules.auth.dto.LogoutRequest;
import com.example.todo_api.modules.auth.dto.MessageResponse;
import com.example.todo_api.modules.auth.dto.RefreshRequest;
import com.example.todo_api.modules.auth.dto.RegisterRequest;
import com.example.todo_api.modules.auth.dto.ResetPasswordRequest;
import com.example.todo_api.modules.auth.dto.TokenResponse;
import com.example.todo_api.modules.auth.dto.VerifyLoginRequest;
import com.example.todo_api.modules.auth.dto.VerifyRegisterRequest;
import com.example.todo_api.modules.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    MessageResponse register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        authService.register(req, ClientIp.of(http));
        return new MessageResponse("A verification code has been sent to your email");
    }

    @PostMapping("/register/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    MessageResponse resendRegister(@Valid @RequestBody EmailRequest req, HttpServletRequest http) {
        authService.resendRegisterOtp(req.email(), ClientIp.of(http));
        return new MessageResponse("A new verification code has been sent");
    }

    @PostMapping("/register/verify")
    @ResponseStatus(HttpStatus.CREATED)
    TokenResponse verifyRegister(@Valid @RequestBody VerifyRegisterRequest req, HttpServletRequest http) {
        return authService.verifyRegister(req.email(), req.otp(), ClientIp.of(http));
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return authService.login(req, ClientIp.of(http));
    }

    @PostMapping("/login/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    MessageResponse resendLogin(@Valid @RequestBody ChallengeRequest req, HttpServletRequest http) {
        authService.resendLoginOtp(req.challengeId(), ClientIp.of(http));
        return new MessageResponse("A new verification code has been sent");
    }

    @PostMapping("/login/verify")
    TokenResponse verifyLogin(@Valid @RequestBody VerifyLoginRequest req, HttpServletRequest http) {
        return authService.verifyLogin(req.challengeId(), req.otp(), ClientIp.of(http));
    }

    @GetMapping("/google/config")
    GoogleConfigResponse googleConfig() {
        return authService.googleConfig();
    }

    @PostMapping("/google")
    TokenResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest req, HttpServletRequest http) {
        return authService.loginWithGoogle(req.credential(), ClientIp.of(http));
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@AuthenticationPrincipal AuthPrincipal principal, @RequestBody(required = false) LogoutRequest req) {
        authService.logout(principal, req == null ? null : req.refreshToken());
    }

    @PostMapping("/change-password")
    TokenResponse changePassword(@AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest req) {
        return authService.changePassword(principal, req);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    MessageResponse forgotPassword(@Valid @RequestBody EmailRequest req, HttpServletRequest http) {
        authService.forgotPassword(req.email(), ClientIp.of(http));
        return new MessageResponse("If the email exists, a password reset link has been sent");
    }

    @PostMapping("/reset-password")
    MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req, HttpServletRequest http) {
        authService.resetPassword(req, ClientIp.of(http));
        return new MessageResponse("Password has been reset, please sign in again");
    }
}
