package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.ChatMessageResponse;
import com.helpdesk.dto.CreateTicketRequest;
import com.helpdesk.dto.TicketPageResponse;
import com.helpdesk.dto.TicketResponse;
import com.helpdesk.dto.UpdateTicketRequest;
import com.helpdesk.model.enums.TicketPriority;
import com.helpdesk.model.enums.TicketStatus;
import com.helpdesk.service.ChatService;
import com.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final ChatService chatService;

    public TicketController(TicketService ticketService, ChatService chatService) {
        this.ticketService = ticketService;
        this.chatService = chatService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'CUSTOMER')")
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request,
                                                 @AuthenticationPrincipal JwtPrincipal principal) {
        TicketResponse response = ticketService.create(principal.getUserId(), request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public TicketPageResponse list(@RequestParam(required = false) TicketStatus status,
                                   @RequestParam(required = false) TicketPriority priority,
                                   @RequestParam(required = false) Long agentId,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @AuthenticationPrincipal JwtPrincipal principal) {
        return ticketService.listForTenant(
                principal.getTenantId(), status, priority, agentId, page, size);
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

    @GetMapping("/{id}/messages")
    public List<ChatMessageResponse> messages(@PathVariable Long id,
                                              @AuthenticationPrincipal JwtPrincipal principal) {
        return chatService.historyForUser(principal, id);
    }

    @GetMapping("/{id}/presence")
    public Map<String, List<Long>> presence(@PathVariable Long id,
                                            @AuthenticationPrincipal JwtPrincipal principal) {
        return Map.of("online", chatService.onlineParticipants(principal, id));
    }
}
