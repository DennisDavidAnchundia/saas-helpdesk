package com.helpdesk.service;

import com.helpdesk.dto.CreateTicketRequest;
import com.helpdesk.dto.TicketPageResponse;
import com.helpdesk.dto.TicketResponse;
import com.helpdesk.dto.UpdateTicketRequest;
import com.helpdesk.model.Tenant;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.model.enums.TicketPriority;
import com.helpdesk.model.enums.TicketStatus;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.TicketSpecifications;
import com.helpdesk.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public TicketService(TicketRepository ticketRepository,
                         UserRepository userRepository,
                         SubscriptionService subscriptionService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public TicketResponse create(Long customerId, CreateTicketRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        subscriptionService.assertCanCreateTicket(customer.getTenant().getId());

        Ticket ticket = new Ticket();
        ticket.setTenant(customer.getTenant());
        ticket.setCustomer(customer);
        ticket.setTitle(request.getTitle().trim());
        ticket.setDescription(request.getDescription().trim());
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        ticket.setSlaDueAt(LocalDateTime.now().plusHours(
                slaResolutionHours(customer.getTenant(), ticket.getPriority())));

        Ticket saved = ticketRepository.save(ticket);
        subscriptionService.registerTicketCreated(customer.getTenant().getId());
        return TicketResponse.from(saved);
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
            throw new IllegalArgumentException("No hay agentes activos en esta empresa");
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

    /**
     * Listado paginado con filtros opcionales (status, priority, agentId).
     * Los filtros null no se aplican; size se limita a [1, 100].
     */
    @Transactional(readOnly = true)
    public TicketPageResponse listForTenant(Long tenantId, TicketStatus status,
                                            TicketPriority priority, Long agentId,
                                            int page, int size) {
        return listForCustomer(tenantId, null, status, priority, agentId, page, size);
    }

    /**
     * Igual que listForTenant pero con scoping opcional por cliente:
     * si customerId != null (rol CUSTOMER) solo ve sus propios tickets.
     */
    @Transactional(readOnly = true)
    public TicketPageResponse listForCustomer(Long tenantId, Long customerId, TicketStatus status,
                                              TicketPriority priority, Long agentId,
                                              int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Ticket> result = ticketRepository.findAll(
                TicketSpecifications.withFilters(tenantId, status, priority, agentId, customerId),
                pageable);
        return TicketPageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public TicketResponse getForTenant(Long tenantId, Long ticketId) {
        return TicketResponse.from(getEntity(tenantId, ticketId));
    }

    /**
     * Detalle para el usuario autenticado: un CUSTOMER solo puede ver
     * sus propios tickets; ADMIN y AGENT ven todo el tenant.
     */
    @Transactional(readOnly = true)
    public TicketResponse getForUser(Long tenantId, Long userId, String role, Long ticketId) {
        Ticket ticket = getEntity(tenantId, ticketId);
        if ("CUSTOMER".equals(role) && !ticket.getCustomer().getId().equals(userId)) {
            throw new IllegalArgumentException("No tienes acceso a este ticket");
        }
        return TicketResponse.from(ticket);
    }

    /** Horas de SLA de resolucion segun prioridad, leidas de la config del tenant. */
    private long slaResolutionHours(Tenant tenant, TicketPriority priority) {
        return switch (priority) {
            case URGENT -> tenant.getSlaUrgentHours();
            case HIGH -> tenant.getSlaHighHours();
            case MEDIUM -> tenant.getSlaMediumHours();
            case LOW -> tenant.getSlaLowHours();
        };
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
                        slaResolutionHours(ticket.getTenant(), request.getPriority())));
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
