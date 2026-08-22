package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.CreateTicketRequest;
import com.helpdesk.dto.TicketResponse;
import com.helpdesk.dto.UpdateTicketRequest;
import com.helpdesk.model.enums.TicketStatus;
import com.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'CUSTOMER')")
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request,
                                                 @AuthenticationPrincipal JwtPrincipal principal) {
        TicketResponse response = ticketService.create(principal.getUserId(), request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public List<TicketResponse> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return ticketService.listForTenant(principal.getTenantId());
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id,
                              @AuthenticationPrincipal JwtPrincipal principal) {
        return ticketService.getForTenant(principal.getTenantId(), id);
    }

    @PutMapping("/{id}")
    public TicketResponse update(@PathVariable Long id,
                                 @Valid @RequestBody UpdateTicketRequest request,
                                 @AuthenticationPrincipal JwtPrincipal principal) {
        return ticketService.update(principal.getTenantId(), id, request);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(@PathVariable Long id,
                                       @RequestParam TicketStatus status,
                                       @AuthenticationPrincipal JwtPrincipal principal) {
        return ticketService.changeStatus(
                principal.getTenantId(),
                principal.getUserId(),
                principal.getRole(),
                id,
                status
        );
    }

    @PatchMapping("/{id}/assign/{agentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public TicketResponse assign(@PathVariable Long id,
                                 @PathVariable Long agentId,
                                 @AuthenticationPrincipal JwtPrincipal principal) {
        return ticketService.assign(principal.getTenantId(), id, agentId);
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public TicketResponse autoAssign(@PathVariable Long id,
                                     @AuthenticationPrincipal JwtPrincipal principal) {
        return ticketService.autoAssign(principal.getTenantId(), id);
    }
}
