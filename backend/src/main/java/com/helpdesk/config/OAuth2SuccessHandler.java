package com.helpdesk.config;

import com.helpdesk.model.User;
import com.helpdesk.service.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

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
                "http://localhost:5173/auth/callback?token=%s&email=%s&fullName=%s&role=%s",
                token,
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );

        response.sendRedirect(redirectUrl);
    }
}
