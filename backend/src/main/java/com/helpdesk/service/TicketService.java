package com.helpdesk.service;

import com.helpdesk.dto.CreateTicketRequest;
import com.helpdesk.dto.TicketResponse;
import com.helpdesk.dto.UpdateTicketRequest;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketService {

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

        return TicketResponse.from(ticketRepository.save(ticket));
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
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    private Ticket getEntity(Long tenantId, Long ticketId) {
        return ticketRepository.findByIdAndTenantId(ticketId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));
    }
}
