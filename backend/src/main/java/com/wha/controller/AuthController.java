package com.wha.controller;

import com.wha.dto.request.LoginRequest;
import com.wha.dto.request.RegisterRequest;
import com.wha.dto.response.ApiResponse;
import com.wha.dto.response.AuthResponse;
import com.wha.security.RateLimitingFilter;
import com.wha.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        String ip = RateLimitingFilter.resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.register(request, ip, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = RateLimitingFilter.resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.login(request, ip, userAgent);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            HttpServletRequest httpRequest) {
        String ip = RateLimitingFilter.resolveClientIp(httpRequest);
        AuthResponse response = authService.refreshTokens(refreshToken, ip);
        return ResponseEntity.ok(ApiResponse.ok("Tokens refreshed", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        String ip = RateLimitingFilter.resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        authService.logoutByEmail(userDetails.getUsername(), ip, userAgent);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(
            @RequestParam String token,
            HttpServletRequest httpRequest) {
        try {
            String ip = RateLimitingFilter.resolveClientIp(httpRequest);
            authService.verifyEmail(token, ip);
            return ResponseEntity.status(302)
                    .header("Location", frontendUrl + "/auth/Verified")
                    .build();
        } catch (Exception ex) {
            String msg = java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.status(302)
                    .header("Location", frontendUrl + "/auth/Verified?error=" + msg)
                    .build();
        }
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {
        String ip = RateLimitingFilter.resolveClientIp(httpRequest);
        AuthResponse response = authService.handleGoogleLogin(body.get("idToken"), ip);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }
}