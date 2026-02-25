package com.example.finset.security.jwt;

import com.example.finset.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CookieService {

    public static final String ACCESS_TOKEN_COOKIE  = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AppProperties appProperties;

    /* ── Write ─────────────────────────────────────────────────── */

    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = build(ACCESS_TOKEN_COOKIE, token,
                (int) (appProperties.getJwt().getAccessExpirationMs() / 1000));
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = build(REFRESH_TOKEN_COOKIE, token,
                (int) (appProperties.getJwt().getRefreshExpirationMs() / 1000));
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /* ── Clear (logout) ────────────────────────────────────────── */

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", build(ACCESS_TOKEN_COOKIE,  "", 0).toString());
        response.addHeader("Set-Cookie", build(REFRESH_TOKEN_COOKIE, "", 0).toString());
    }

    /* ── Read ──────────────────────────────────────────────────── */

    public Optional<String> getAccessToken(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    public Optional<String> getRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    /* ── Builder ───────────────────────────────────────────────── */

    private ResponseCookie build(String name, String value, int maxAgeSec) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(appProperties.getCookie().isSecure())
                .path("/")
                .maxAge(maxAgeSec)
                .sameSite(appProperties.getCookie().getSameSite())
                .build();
    }
}