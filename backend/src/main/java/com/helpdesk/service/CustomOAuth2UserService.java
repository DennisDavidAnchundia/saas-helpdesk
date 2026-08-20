package com.helpdesk.service;

import com.helpdesk.model.Tenant;
import com.helpdesk.model.User;
import com.helpdesk.model.enums.Provider;
import com.helpdesk.model.enums.Role;
import com.helpdesk.repository.TenantRepository;
import com.helpdesk.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    public CustomOAuth2UserService(UserRepository userRepository,
                                    TenantRepository tenantRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oauth2User.getName();

        Provider provider = Provider.valueOf(registrationId.toUpperCase());

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> createOAuthUser(email, name, picture, provider, providerId));

        return new CustomOAuth2User(oauth2User, user);
    }

    private User createOAuthUser(String email, String name, String picture,
                                  Provider provider, String providerId) {
        Tenant tenant = createOAuthTenant(email);

        User user = new User(
                tenant,
                email,
                name,
                picture,
                Role.ADMIN,
                provider,
                providerId
        );
        return userRepository.save(user);
    }

    private Tenant createOAuthTenant(String email) {
        String domain = email.split("@")[1];
        String tenantName = domain.substring(0, domain.indexOf('.')).toUpperCase();
        String slug = tenantName.toLowerCase().replaceAll("[^a-z0-9]", "");

        return tenantRepository.findBySlug(slug)
                .orElseGet(() -> {
                    Tenant tenant = new Tenant(tenantName, slug);
                    return tenantRepository.save(tenant);
                });
    }
}
