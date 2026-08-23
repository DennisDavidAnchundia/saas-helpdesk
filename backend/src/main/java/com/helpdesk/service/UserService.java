package com.helpdesk.service;

import com.helpdesk.dto.AgentResponse;
import com.helpdesk.dto.UserResponse;
import com.helpdesk.model.User;
import com.helpdesk.model.enums.Role;
import com.helpdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
