package com.helpdesk.service;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.ChatMessageResponse;
import com.helpdesk.dto.SendChatMessageRequest;
import com.helpdesk.model.Message;
import com.helpdesk.model.MessageRead;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.repository.MessageReadRepository;
import com.helpdesk.repository.MessageRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final MessageReadRepository messageReadRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final PresenceService presenceService;

    public ChatService(MessageRepository messageRepository,
                       MessageReadRepository messageReadRepository,
                       TicketRepository ticketRepository,
                       UserRepository userRepository,
                       PresenceService presenceService) {
        this.messageRepository = messageRepository;
        this.messageReadRepository = messageReadRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.presenceService = presenceService;
    }

    @Transactional
    public ChatMessageResponse send(JwtPrincipal principal, Long ticketId, SendChatMessageRequest request) {
        Ticket ticket = accessibleTicket(principal, ticketId);

        User sender = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Message message = new Message();
        message.setTenant(ticket.getTenant());
        message.setTicket(ticket);
        message.setSender(sender);
        message.setContent(request.getContent().trim());

        return ChatMessageResponse.from(messageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> historyForUser(JwtPrincipal principal, Long ticketId) {
        Ticket ticket = accessibleTicket(principal, ticketId);
        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> onlineParticipants(JwtPrincipal principal, Long ticketId) {
        Ticket ticket = accessibleTicket(principal, ticketId);
        return presenceService.onlineUsers(ticket.getTenant().getId());
    }

    /** Marca la conversacion como leida hasta ahora para este usuario (upsert). */
    @Transactional
    public void markRead(JwtPrincipal principal, Long ticketId) {
        Ticket ticket = accessibleTicket(principal, ticketId);

        MessageRead read = messageReadRepository
                .findByTicketIdAndUserId(ticket.getId(), principal.getUserId())
                .orElseGet(() -> {
                    MessageRead fresh = new MessageRead();
                    fresh.setTicket(ticket);
                    fresh.setUser(userRepository.getReferenceById(principal.getUserId()));
                    return fresh;
                });
        read.setLastReadAt(LocalDateTime.now());
        messageReadRepository.save(read);
    }

    /**
     * Mapa ticketId -> mensajes no leidos, filtrado por participacion:
     * ADMIN ve todo el tenant, AGENT solo lo asignado, CUSTOMER solo lo propio.
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> unreadCounts(JwtPrincipal principal) {
        List<Object[]> rows = messageReadRepository.countUnreadByUser(
                principal.getUserId(), principal.getTenantId(), principal.getRole());
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return counts;
    }

    public Ticket accessibleTicket(JwtPrincipal principal, Long ticketId) {
        Ticket ticket = ticketRepository.findByIdAndTenantId(ticketId, principal.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        boolean member = switch (principal.getRole()) {
            case "ADMIN" -> true;
            case "AGENT" -> ticket.getAgent() != null
                    && ticket.getAgent().getId().equals(principal.getUserId());
            default -> ticket.getCustomer().getId().equals(principal.getUserId());
        };

        if (!member) {
            throw new IllegalArgumentException("No participas de este ticket");
        }
        return ticket;
    }
}
