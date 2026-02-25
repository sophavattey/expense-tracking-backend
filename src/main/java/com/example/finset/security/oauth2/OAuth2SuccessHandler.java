package com.example.finset.security.oauth2;

import com.example.finset.config.AppProperties;
import com.example.finset.entity.User;
import com.example.finset.service.AuthService;
import com.example.finset.service.UserService;
import com.example.finset.security.jwt.CookieService;
import com.example.finset.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final AuthService authService;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email      = oAuth2User.getAttribute("email");
        String name       = oAuth2User.getAttribute("name");
        String picture    = oAuth2User.getAttribute("picture");
        String providerId = oAuth2User.getAttribute("sub");  // Google's unique user ID

        log.info("OAuth2 login: email={}", email);

        // Find or create user in our DB
        User user = userService.findOrCreateGoogleUser(email, name, picture, providerId);

        // Issue our own JWT pair
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = authService.createRefreshToken(user);

        // Write both as HTTP-only cookies
        cookieService.setAccessTokenCookie(response, accessToken);
        cookieService.setRefreshTokenCookie(response, refreshToken);

        // Redirect to dashboard
        String redirectUrl = appProperties.getFrontendUrl() + "/dashboard";
        log.info("Redirecting OAuth2 user to {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}