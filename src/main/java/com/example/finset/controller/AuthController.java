package com.example.finset.controller;

import com.example.finset.dto.request.LoginRequest;
import com.example.finset.dto.request.RegisterRequest;
import com.example.finset.dto.response.ApiResponse;
import com.example.finset.dto.response.AuthResponse;
import com.example.finset.entity.User;
import com.example.finset.repository.UserRepository;
import com.example.finset.security.jwt.CookieService;
import com.example.finset.security.jwt.JwtService;
import com.example.finset.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final UserRepository userRepository;

    /* ── POST /api/auth/register ────────────────────────────────── */

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req,
                                              HttpServletResponse response) {
        User user = authService.register(req);
        issueTokens(user, response);
        log.info("New user registered: {}", user.getEmail());
        return ApiResponse.ok("Account created successfully", toDto(user));
    }

    /* ── POST /api/auth/login ───────────────────────────────────── */

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req,
                                           HttpServletResponse response) {
        User user = authService.login(req);
        issueTokens(user, response);
        log.info("User logged in: {}", user.getEmail());
        return ApiResponse.ok("Login successful", toDto(user));
    }

    /* ── POST /api/auth/refresh ─────────────────────────────────── */

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(HttpServletRequest request,
                                             HttpServletResponse response) {
        String rawRefreshToken = cookieService.getRefreshToken(request)
                .orElseThrow(() -> new com.example.finset.exception.TokenRefreshException(
                        "No refresh token cookie found"));

        String[] newRefreshHolder = new String[1];
        User user = authService.validateAndRotateRefreshToken(rawRefreshToken, newRefreshHolder);

        // Issue new access token
        String newAccessToken = jwtService.generateAccessToken(user);
        cookieService.setAccessTokenCookie(response, newAccessToken);
        // Rotated refresh token
        cookieService.setRefreshTokenCookie(response, newRefreshHolder[0]);

        log.debug("Tokens refreshed for user: {}", user.getEmail());
        return ApiResponse.ok("Tokens refreshed", toDto(user));
    }

    /* ── POST /api/auth/logout ──────────────────────────────────── */

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal UserDetails userDetails,
                                    HttpServletResponse response) {
        if (userDetails != null) {
            Long userId = Long.parseLong(userDetails.getUsername());
            userRepository.findById(userId).ifPresent(authService::revokeAllTokens);
            log.info("User {} logged out", userId);
        }
        cookieService.clearAuthCookies(response);
        return ApiResponse.ok("Logged out successfully", null);
    }

    /* ── GET /api/auth/me ───────────────────────────────────────── */

    @GetMapping("/me")
    public ApiResponse<AuthResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ApiResponse.ok(toDto(user));
    }

    /* ── Helpers ────────────────────────────────────────────────── */

    private void issueTokens(User user, HttpServletResponse response) {
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = authService.createRefreshToken(user);
        cookieService.setAccessTokenCookie(response, accessToken);
        cookieService.setRefreshTokenCookie(response, refreshToken);
    }

    private AuthResponse toDto(User user) {
        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .provider(user.getProvider().name())
                .build();
    }
}