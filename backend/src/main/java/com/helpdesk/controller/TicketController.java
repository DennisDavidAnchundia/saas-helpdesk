package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.ChatMessageResponse;
import com.helpdesk.dto.CreateTicketRequest;
import com.helpdesk.dto.OnlineUserResponse;
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
                                   @RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @AuthenticationPrincipal JwtPrincipal principal) {
        // Un CUSTOMER solo ve sus propios tickets; ADMIN/AGENT ven todo el tenant
        Long customerId = "CUSTOMER".equals(principal.getRole()) ? principal.getUserId() : null;
        return ticketService.listForCustomer(
                principal.getTenantId(), customerId, status, priority, agentId, page, size, q);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id,
                              @AuthenticationPrincipal JwtPrincipal principal) {
        return ticketService.getForUser(
                principal.getTenantId(), principal.getUserId(), principal.getRole(), id);
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
        return ticketService.assign(
                principal.getTenantId(), id, agentId, principal.getRole(), principal.getUserId());
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public TicketResponse autoAssign(@PathVariable Long id,
                                     @AuthenticationPrincipal JwtPrincipal principal) {
        return ticketService.autoAssign(principal.getTenantId(), id, principal.getRole(), principal.getUserId());
    }

    @GetMapping("/{id}/messages")
    public List<ChatMessageResponse> messages(@PathVariable Long id,
                                              @AuthenticationPrincipal JwtPrincipal principal) {
        return chatService.historyForUser(principal, id);
    }

    @GetMapping("/{id}/presence")
    public Map<String, List<OnlineUserResponse>> presence(@PathVariable Long id,
                                                          @AuthenticationPrincipal JwtPrincipal principal) {
        return Map.of("online", chatService.onlineParticipantsDetailed(principal, id));
    }

    /** Mapa ticketId -> cantidad de mensajes no leidos para el usuario actual. */
    @GetMapping("/unread")
    public Map<Long, Long> unread(@AuthenticationPrincipal JwtPrincipal principal) {
        return chatService.unreadCounts(principal);
    }

    /** Marca la conversacion como leida (se llama al abrir/seleccionar el ticket en el chat). */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id,
                                         @AuthenticationPrincipal JwtPrincipal principal) {
        chatService.markRead(principal, id);
        return ResponseEntity.noContent().build();
    }
}
