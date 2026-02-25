package com.example.finset.service;

import com.example.finset.config.AppProperties;
import com.example.finset.dto.request.LoginRequest;
import com.example.finset.dto.request.RegisterRequest;
import com.example.finset.entity.RefreshToken;
import com.example.finset.entity.User;
import com.example.finset.exception.EmailAlreadyExistsException;
import com.example.finset.exception.InvalidCredentialsException;
import com.example.finset.exception.TokenRefreshException;
import com.example.finset.repository.RefreshTokenRepository;
import com.example.finset.repository.UserRepository;
import com.example.finset.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    /* ── Register ───────────────────────────────────────────────── */

    @Transactional
    public User register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new EmailAlreadyExistsException(req.getEmail());
        }
        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .provider(User.AuthProvider.LOCAL)
                .build();
        return userRepository.save(user);
    }

    /* ── Login ──────────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public User login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException());

        if (user.getPassword() == null ||
            !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    /* ── Refresh token lifecycle ────────────────────────────────── */

    /**
     * Creates a new refresh token, persists a HASH of it, returns the raw value.
     * The raw value goes into the HTTP-only cookie; the hash lives in the DB.
     */
    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = jwtService.generateRefreshToken(user);
        String tokenHash = sha256(rawToken);

        Instant expiresAt = Instant.now().plusMillis(
                appProperties.getJwt().getRefreshExpirationMs());

        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(expiresAt)
                .build());

        return rawToken;
    }

    /**
     * Validates the refresh token cookie:
     * 1. Parse the JWT signature
     * 2. Look up the hash in the DB
     * 3. Check it isn't revoked or expired
     * Returns the user if valid.
     */
    @Transactional
    public User validateAndRotateRefreshToken(String rawToken,
                                              String[] newRefreshTokenHolder) {
        // 1. Verify JWT signature & expiry
        Claims claims;
        try {
            claims = jwtService.validateRefreshToken(rawToken);
        } catch (JwtException ex) {
            throw new TokenRefreshException("Invalid or expired refresh token");
        }

        // 2. Check DB record
        String tokenHash = sha256(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenRefreshException("Refresh token not found"));

        if (stored.isRevoked()) {
            // Possible token reuse — revoke ALL tokens for this user (security measure)
            refreshTokenRepository.revokeAllByUser(stored.getUser());
            throw new TokenRefreshException("Refresh token has been revoked");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new TokenRefreshException("Refresh token has expired");
        }

        // 3. Rotate: revoke old, issue new
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        String newRaw = createRefreshToken(user);
        newRefreshTokenHolder[0] = newRaw;

        return user;
    }

    /* ── Logout ─────────────────────────────────────────────────── */

    @Transactional
    public void revokeAllTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    /* ── Util ───────────────────────────────────────────────────── */

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}