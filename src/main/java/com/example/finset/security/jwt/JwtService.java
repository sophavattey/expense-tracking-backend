package com.example.finset.security.jwt;

import com.example.finset.config.AppProperties;
import com.example.finset.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final AppProperties appProperties;

    /* ── Access token (short-lived, 15 min) ────────────────────── */

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name",  user.getName())
                .claim("role",  user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()
                        + appProperties.getJwt().getAccessExpirationMs()))
                .signWith(accessKey())
                .compact();
    }

    public Claims validateAccessToken(String token) {
        return parse(token, accessKey());
    }

    /* ── Refresh token (long-lived, 30 days) ───────────────────── */

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()
                        + appProperties.getJwt().getRefreshExpirationMs()))
                .signWith(refreshKey())
                .compact();
    }

    public Claims validateRefreshToken(String token) {
        return parse(token, refreshKey());
    }

    /* ── Helpers ────────────────────────────────────────────────── */

    private Claims parse(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey accessKey() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(appProperties.getJwt().getAccessSecret()));
    }

    private SecretKey refreshKey() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(appProperties.getJwt().getRefreshSecret()));
    }
}