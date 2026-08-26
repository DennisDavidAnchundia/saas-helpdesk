package com.helpdesk.config;

import com.helpdesk.model.User;
import com.helpdesk.service.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Value("${app.oauth2.redirect-url:http://localhost:5173}")
    private String frontendUrl;

    public OAuth2SuccessHandler(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oauth2User.getUser();

        String token = jwtProvider.generateToken(
                user.getId(),
                user.getTenant().getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String redirectUrl = String.format(
                "%s/auth/callback?token=%s&email=%s&fullName=%s&role=%s",
                frontendUrl,
                URLEncoder.encode(token, StandardCharsets.UTF_8),
                URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8),
                URLEncoder.encode(user.getFullName(), StandardCharsets.UTF_8),
                URLEncoder.encode(user.getRole().name(), StandardCharsets.UTF_8)
        );

        response.sendRedirect(redirectUrl);
    }
}