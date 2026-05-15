package com.wha.service;

import com.wha.dto.request.LoginRequest;
import com.wha.dto.request.RegisterRequest;
import com.wha.dto.response.AuthResponse;
import com.wha.dto.response.UserResponse;
import com.wha.entity.RefreshToken;
import com.wha.entity.User;
import com.wha.exception.AppException;
import com.wha.repository.RefreshTokenRepository;
import com.wha.repository.SubscriptionRepository;
import com.wha.repository.UserRepository;
import com.wha.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final GoogleOAuthService googleOAuthService;
    private final EmailService emailService;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    @Value("${app.security.max-failed-login-attempts}")
    private int maxFailedAttempts;

    @Value("${app.security.lockout-duration-minutes}")
    private int lockoutMinutes;

    @Value("${app.verification.token-expiry-hours}")
    private int verificationTokenExpiryHours;

    @Transactional
    public AuthResponse register(RegisterRequest request, String ip, String userAgent) {
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw AppException.conflict("An account with this email already exists");
        }

        long accountsFromIp = userRepository.countAccountsCreatedFromIp(
                ip, LocalDateTime.now().minusHours(24));
        if (accountsFromIp >= 3) {
            auditService.flagSuspicious(null, request.email(), "MASS_REGISTRATION", ip,
                    "3+ accounts registered from same IP in 24h");
        }

        // Generate verification token
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        User user = User.builder()
                .email(request.email().toLowerCase().strip())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().strip())
                .lastName(request.lastName().strip())
                .phone(request.phone())
                .role(User.Role.USER)
                .active(false)                    // inactive until email verified
                .emailVerified(false)
                .verificationToken(tokenHash)
                .verificationTokenExpiresAt(LocalDateTime.now().plusHours(verificationTokenExpiryHours))
                .build();

        userRepository.save(user);
        auditService.log(user.getId(), user.getEmail(), "REGISTER", ip, userAgent, null);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), rawToken);

        // Return a minimal response — no access token yet since not verified
        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .user(UserResponse.from(user, false))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ip, String userAgent) {
        auditService.checkBruteForce(ip, request.email());

        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseGet(() -> {
                    passwordEncoder.encode("dummy_to_prevent_timing_attack");
                    auditService.log(null, request.email(), "LOGIN_FAILED", ip, userAgent,
                            "Email not found");
                    throw AppException.unauthorized("Invalid email or password");
                });

        if (user.isLocked()) {
            auditService.log(user.getId(), user.getEmail(), "LOGIN_BLOCKED", ip, userAgent,
                    "Account locked until " + user.getLockedUntil());
            throw AppException.forbidden("Account is temporarily locked. Try again later.");
        }

        if (!user.isEmailVerified()) {
            auditService.log(user.getId(), user.getEmail(), "LOGIN_BLOCKED", ip, userAgent,
                    "Email not verified");
            throw AppException.forbidden("Please verify your email address before signing in. Check your inbox.");
        }

        if (!user.isActive()) {
            auditService.log(user.getId(), user.getEmail(), "LOGIN_BLOCKED", ip, userAgent,
                    "Account deactivated");
            throw AppException.forbidden("This account has been deactivated. Contact support.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= maxFailedAttempts) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutMinutes));
                auditService.flagSuspicious(user.getId(), user.getEmail(),
                        "ACCOUNT_LOCKED", ip, "Max failed login attempts reached");
            }

            userRepository.save(user);
            auditService.log(user.getId(), user.getEmail(), "LOGIN_FAILED", ip, userAgent,
                    "Wrong password, attempt " + attempts);
            throw AppException.unauthorized("Invalid email or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userRepository.save(user);

        auditService.log(user.getId(), user.getEmail(), "LOGIN_SUCCESS", ip, userAgent, null);
        return buildAuthResponse(user, ip);
    }

    @Transactional
    public AuthResponse verifyEmail(String token, String ip) {
        String tokenHash = hashToken(token);
        User user = userRepository.findByVerificationToken(tokenHash)
                .orElseThrow(() -> AppException.badRequest("Invalid or expired verification link"));

        if (user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw AppException.badRequest("Verification link has expired. Please register again.");
        }

        // Idempotent: works even if called twice (React Strict Mode fires effects twice)
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setActive(true);
            // Keep token in DB until it naturally expires (24h) — allows safe retry
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userRepository.save(user);

        return buildAuthResponse(user, ip);
    }

    @Transactional
    public AuthResponse handleGoogleLogin(String idToken, String ip) {
        GoogleOAuthService.GoogleUserInfo googleUser = googleOAuthService.verifyIdToken(idToken);

        // Existing user
        if (userRepository.existsByEmail(googleUser.email())) {
            User user = userRepository.findByEmail(googleUser.email()).get();

            if (!user.isEmailVerified()) {
                // Resend verification
                String rawToken = generateSecureToken();
                user.setVerificationToken(hashToken(rawToken));
                user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(verificationTokenExpiryHours));
                userRepository.save(user);
                emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), rawToken);
                return AuthResponse.builder().accessToken(null).refreshToken(null)
                        .user(UserResponse.from(user, false)).build();
            }

            if (!user.isActive()) {
                throw AppException.forbidden("This account has been deactivated.");
            }

            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ip);
            userRepository.save(user);
            auditService.log(user.getId(), user.getEmail(), "GOOGLE_LOGIN", ip, null, null);
            return buildAuthResponse(user, ip);
        }

        // New user — create account, require email verification
        String rawToken = generateSecureToken();
        User newUser = User.builder()
                .email(googleUser.email())
                .firstName(googleUser.firstName() != null ? googleUser.firstName() : "")
                .lastName(googleUser.lastName() != null ? googleUser.lastName() : "")
                .passwordHash(passwordEncoder.encode(generateSecureToken()))
                .role(User.Role.USER)
                .active(false)
                .emailVerified(false)
                .verificationToken(hashToken(rawToken))
                .verificationTokenExpiresAt(LocalDateTime.now().plusHours(verificationTokenExpiryHours))
                .build();
        userRepository.save(newUser);
        auditService.log(newUser.getId(), newUser.getEmail(), "GOOGLE_REGISTER", ip, null, null);
        emailService.sendVerificationEmail(newUser.getEmail(), newUser.getFirstName(), rawToken);

        return AuthResponse.builder().accessToken(null).refreshToken(null)
                .user(UserResponse.from(newUser, false)).build();
    }

    @Transactional
    public AuthResponse refreshTokens(String rawToken, String ip) {
        String tokenHash = hashToken(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> AppException.unauthorized("Invalid refresh token"));

        if (!stored.isValid()) {
            refreshTokenRepository.revokeAllByUserId(stored.getUser().getId());
            auditService.flagSuspicious(stored.getUser().getId(), stored.getUser().getEmail(),
                    "REFRESH_TOKEN_REUSE", ip, "Expired or revoked refresh token used");
            throw AppException.unauthorized("Refresh token is invalid or expired");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildAuthResponse(stored.getUser(), ip);
    }

    @Transactional
    public void logoutByEmail(String email, String ip, String userAgent) {
        userRepository.findByEmail(email).ifPresent(user -> {
            refreshTokenRepository.revokeAllByUserId(user.getId());
            auditService.log(user.getId(), email, "LOGOUT", ip, userAgent, null);
        });
    }

    private AuthResponse buildAuthResponse(User user, String ip) {
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());

        String rawRefreshToken = generateSecureToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(LocalDateTime.now().plusNanos(refreshTokenExpiryMs * 1_000_000L))
                .issuedToIp(ip)
                .build();
        refreshTokenRepository.save(refreshToken);

        boolean hasSub = subscriptionRepository.findByUserId(user.getId())
                .map(s -> s.isActive()).orElse(false);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .user(UserResponse.from(user, hasSub))
                .build();
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}