package com.helpdesk.service;

import com.helpdesk.config.JwtProvider;
import com.helpdesk.dto.ChangePasswordRequest;
import com.helpdesk.dto.JoinRequest;
import com.helpdesk.dto.LoginRequest;
import com.helpdesk.dto.LoginResponse;
import com.helpdesk.dto.RegisterRequest;
import com.helpdesk.dto.RegisterResponse;
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
    private final JwtProvider jwtProvider;

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
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
                slug,
                user.getCreatedAt()
        );
    }

    /**
     * Registro publico como CUSTOMER de una empresa existente (portal de clientes).
     * El rol se fuerza a CUSTOMER en el servidor.
     */
    @Transactional
    public RegisterResponse join(JoinRequest request) {
        Tenant tenant = tenantRepository.findBySlug(request.getTenantSlug())
                .orElseThrow(() -> new IllegalArgumentException("No existe una empresa con ese slug"));

        if (userRepository.existsByTenantIdAndEmail(tenant.getId(), request.getEmail())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email en la empresa");
        }

        User user = new User(
                tenant,
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                Role.CUSTOMER
        );
        user = userRepository.save(user);

        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                user.getCreatedAt()
        );
    }

    public LoginResponse login(LoginRequest request) {
        Tenant tenant = tenantRepository.findBySlug(request.getTenantSlug())
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));

        User user = userRepository.findByTenantIdAndEmail(tenant.getId(), request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Email o contraseña incorrectos");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("La cuenta está desactivada");
        }

        String token = jwtProvider.generateToken(
                user.getId(),
                tenant.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                tenant.getId(),
                tenant.getName()
        );
    }

    /**
     * Cambio de contraseña propia. Requiere la actual para confirmar identidad;
     * las cuentas de Google (sin password local) no pueden usar este flujo.
     */
    @Transactional
    public void changeOwnPassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.getPassword() == null) {
            throw new IllegalArgumentException(
                    "Esta cuenta inicia sesion con Google y no tiene contrasena local");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La contrasena actual no es correcta");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
