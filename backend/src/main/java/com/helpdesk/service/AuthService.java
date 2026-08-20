package com.helpdesk.service;

import com.helpdesk.dto.RegisterRequest;
import com.helpdesk.dto.RegisterResponse;
import com.helpdesk.exception.ResourceNotFoundException;
import com.helpdesk.model.Tenant;
import com.helpdesk.model.User;
import com.helpdesk.model.enums.Role;
import com.helpdesk.repository.TenantRepository;
import com.helpdesk.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String slug = generateSlug(request.getTenantName());

        if (tenantRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Ya existe una empresa con ese nombre");
        }

        Tenant tenant = new Tenant(request.getTenantName(), slug);
        tenant = tenantRepository.save(tenant);

        if (userRepository.existsByTenantIdAndEmail(tenant.getId(), request.getEmail())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
                tenant,
                request.getEmail(),
                encodedPassword,
                request.getFullName(),
                Role.ADMIN
        );
        user = userRepository.save(user);

        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                tenant.getId(),
                tenant.getName(),
                user.getCreatedAt()
        );
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
