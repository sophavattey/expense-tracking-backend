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

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService    authService;
    private final JwtService     jwtService;
    private final CookieService  cookieService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req,
                                              HttpServletResponse response) {
        User user = authService.register(req);
        issueTokens(user, response);
        log.info("New user registered: {}", user.getEmail());
        return ApiResponse.ok("Account created successfully", toDto(user));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req,
                                           HttpServletResponse response) {
        User user = authService.login(req);
        issueTokens(user, response);
        log.info("User logged in: {}", user.getEmail());
        return ApiResponse.ok("Login successful", toDto(user));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(HttpServletRequest request,
                                             HttpServletResponse response) {
        String rawRefreshToken = cookieService.getRefreshToken(request)
                .orElseThrow(() -> new com.example.finset.exception.TokenRefreshException(
                        "No refresh token cookie found"));

        String[] newRefreshHolder = new String[1];
        User user = authService.validateAndRotateRefreshToken(rawRefreshToken, newRefreshHolder);

        cookieService.setAccessTokenCookie(response, jwtService.generateAccessToken(user));
        cookieService.setRefreshTokenCookie(response, newRefreshHolder[0]);

        log.debug("Tokens refreshed for user: {}", user.getEmail());
        return ApiResponse.ok("Tokens refreshed", toDto(user));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal UserDetails userDetails,
                                    HttpServletResponse response) {
        if (userDetails != null) {
            UUID userId = UUID.fromString(userDetails.getUsername());
            userRepository.findById(userId).ifPresent(authService::revokeAllTokens);
            log.info("User {} logged out", userId);
        }
        cookieService.clearAuthCookies(response);
        return ApiResponse.ok("Logged out successfully", null);
    }

    @GetMapping("/me")
    public ApiResponse<AuthResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ApiResponse.ok(toDto(user));
    }

    private void issueTokens(User user, HttpServletResponse response) {
        cookieService.setAccessTokenCookie(response, jwtService.generateAccessToken(user));
        cookieService.setRefreshTokenCookie(response, authService.createRefreshToken(user));
    }

    // preferredCurrency now included in every auth response
    AuthResponse toDto(User user) {
        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .provider(user.getProvider().name())
                .preferredCurrency(user.getPreferredCurrency())   // ✅ NEW
                .build();
    }
}