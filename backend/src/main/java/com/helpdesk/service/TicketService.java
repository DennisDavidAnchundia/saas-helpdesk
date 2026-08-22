package com.helpdesk.service;

import com.helpdesk.dto.CreateTicketRequest;
import com.helpdesk.dto.TicketResponse;
import com.helpdesk.dto.UpdateTicketRequest;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.model.enums.TicketPriority;
import com.helpdesk.model.enums.TicketStatus;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {

    // Horas maximas de resolucion segun prioridad (politica SLA)
    private static final Map<TicketPriority, Long> SLA_RESOLUTION_HOURS = Map.of(
            TicketPriority.URGENT, 4L,
            TicketPriority.HIGH, 8L,
            TicketPriority.MEDIUM, 24L,
            TicketPriority.LOW, 72L
    );

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TicketResponse create(Long customerId, CreateTicketRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Ticket ticket = new Ticket();
        ticket.setTenant(customer.getTenant());
        ticket.setCustomer(customer);
        ticket.setTitle(request.getTitle().trim());
        ticket.setDescription(request.getDescription().trim());
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        ticket.setSlaDueAt(LocalDateTime.now().plusHours(
                SLA_RESOLUTION_HOURS.get(ticket.getPriority())));

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse assign(Long tenantId, Long ticketId, Long agentId) {
        Ticket ticket = getEntity(tenantId, ticketId);
        User agent = getActiveAgent(tenantId, agentId);
        ticket.setAgent(agent);
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse autoAssign(Long tenantId, Long ticketId) {
        List<User> agents = userRepository.findActiveAgentsByTenant(tenantId);
        if (agents.isEmpty()) {
            throw new IllegalStateException("No hay agentes activos en esta empresa");
        }

        User selected = agents.stream()
                .min(java.util.Comparator.comparingLong(agent ->
                        ticketRepository.countByAgentIdAndStatusIn(
                                agent.getId(),
                                java.util.List.of(TicketStatus.OPEN,
                                        TicketStatus.IN_PROGRESS,
                                        TicketStatus.REOPENED))))
                .orElseThrow();

        Ticket ticket = getEntity(tenantId, ticketId);
        ticket.setAgent(selected);
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    private User getActiveAgent(Long tenantId, Long agentId) {
        return userRepository.findActiveAgentsByTenant(tenantId).stream()
                .filter(u -> u.getId().equals(agentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Agente no encontrado o inactivo en esta empresa"));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listForTenant(Long tenantId) {
        return ticketRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getForTenant(Long tenantId, Long ticketId) {
        return TicketResponse.from(getEntity(tenantId, ticketId));
    }

    @Transactional
    public TicketResponse update(Long tenantId, Long ticketId, UpdateTicketRequest request) {
        Ticket ticket = getEntity(tenantId, ticketId);
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            ticket.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            ticket.setDescription(request.getDescription().trim());
        }
        if (request.getPriority() != null && request.getPriority() != ticket.getPriority()) {
            ticket.setPriority(request.getPriority());
            // Recalcular SLA si el ticket sigue activo
            if (ticket.getStatus() != TicketStatus.RESOLVED
                    && ticket.getStatus() != TicketStatus.CLOSED) {
                ticket.setSlaDueAt(LocalDateTime.now().plusHours(
                        SLA_RESOLUTION_HOURS.get(request.getPriority())));
            }
        }
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse changeStatus(Long tenantId, Long userId, String role,
                                       Long ticketId, TicketStatus target) {
        Ticket ticket = getEntity(tenantId, ticketId);

        if (!ticket.getStatus().canTransitionTo(target)) {
            throw new IllegalArgumentException(
                    "Transicion invalida: " + ticket.getStatus() + " -> " + target);
        }

        if ("CUSTOMER".equals(role)) {
            if (target != TicketStatus.REOPENED) {
                throw new IllegalArgumentException("Un cliente solo puede reabrir tickets");
            }
            if (!ticket.getCustomer().getId().equals(userId)) {
                throw new IllegalArgumentException("No puedes reabrir un ticket de otro usuario");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        ticket.setStatus(target);

        if (target == TicketStatus.IN_PROGRESS && ticket.getFirstResponseAt() == null) {
            ticket.setFirstResponseAt(now);
        }
        if (target == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(now);
        }
        if (target == TicketStatus.CLOSED) {
            ticket.setClosedAt(now);
        }
        if (target == TicketStatus.REOPENED) {
            ticket.setResolvedAt(null);
            ticket.setClosedAt(null);
        }

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    private Ticket getEntity(Long tenantId, Long ticketId) {
        return ticketRepository.findByIdAndTenantId(ticketId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));
    }
}
