package com.helpdesk.service;

import com.helpdesk.dto.AgentResponse;
import com.helpdesk.dto.CreateUserRequest;
import com.helpdesk.dto.UserResponse;
import com.helpdesk.model.Tenant;
import com.helpdesk.model.User;
import com.helpdesk.model.enums.Role;
import com.helpdesk.repository.TenantRepository;
import com.helpdesk.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Agentes activos del tenant (rol AGENT), para asignacion manual de tickets. */
    @Transactional(readOnly = true)
    public List<AgentResponse> listActiveAgents(Long tenantId) {
        return userRepository.findActiveAgentsByTenant(tenantId).stream()
                .map(AgentResponse::from)
                .toList();
    }

    /** Todos los usuarios del tenant, para el panel admin. */
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(Long tenantId) {
        return userRepository.findByTenantIdOrderByIdAsc(tenantId).stream()
                .map(UserResponse::from)
                .toList();
    }

    /**
     * Crea un agente dentro del tenant (panel admin). El rol siempre es AGENT:
     * el servidor lo ignora si el cliente intenta enviar otro.
     */
    @Transactional
    public UserResponse createAgent(Long tenantId, CreateUserRequest request) {
        if (userRepository.existsByTenantIdAndEmail(tenantId, request.getEmail())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email en la empresa");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
        User agent = new User(
                tenant,
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                Role.AGENT
        );
        return UserResponse.from(userRepository.save(agent));
    }

    /**
     * Activa o desactiva un usuario del tenant. Por politica solo se
     * permiten toggles sobre AGENT: admins y customers no se tocan aqui.
     */
    @Transactional
    public UserResponse setUserActive(Long tenantId, Long userId, boolean active) {
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado en esta empresa"));
        if (user.getRole() != Role.AGENT) {
            throw new IllegalArgumentException("Solo se pueden activar o desactivar agentes");
        }
        user.setActive(active);
        return UserResponse.from(userRepository.save(user));
    }
}
