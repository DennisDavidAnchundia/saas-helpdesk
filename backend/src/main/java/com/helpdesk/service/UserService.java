package com.helpdesk.service;

import com.helpdesk.dto.AgentResponse;
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
}
