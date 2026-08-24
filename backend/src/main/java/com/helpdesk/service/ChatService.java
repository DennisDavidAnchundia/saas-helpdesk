package com.helpdesk.service;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.ChatMessageResponse;
import com.helpdesk.dto.ChatPageResponse;
import com.helpdesk.dto.OnlineUserResponse;
import com.helpdesk.dto.SendChatMessageRequest;
import com.helpdesk.model.Message;
import com.helpdesk.model.MessageRead;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.repository.MessageReadRepository;
import com.helpdesk.repository.MessageRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;
import com.helpdesk.service.event.Notifications;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final ApplicationEventPublisher events;

    public ChatService(MessageRepository messageRepository,
                       MessageReadRepository messageReadRepository,
                       TicketRepository ticketRepository,
                       UserRepository userRepository,
                       PresenceService presenceService,
                       ApplicationEventPublisher events) {
        this.messageRepository = messageRepository;
        this.messageReadRepository = messageReadRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.presenceService = presenceService;
        this.events = events;
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

        ChatMessageResponse response = ChatMessageResponse.from(messageRepository.save(message));

        // Si el cliente escribe y hay agente asignado, el agente recibe un aviso por email
        if ("CUSTOMER".equals(principal.getRole())
                && ticket.getAgent() != null
                && !ticket.getAgent().getId().equals(sender.getId())) {
            String preview = response.getContent();
            if (preview.length() > 80) {
                preview = preview.substring(0, 80) + "…";
            }
            events.publishEvent(new Notifications.CustomerReplied(
                    ticket.getId(),
                    ticket.getTitle(),
                    ticket.getAgent().getEmail(),
                    ticket.getAgent().getFullName(),
                    sender.getFullName(),
                    preview));
        }

        return response;
    }

    /**
     * Historial paginado: la pagina 0 trae los mensajes mas recientes y el
     * contenido de cada pagina viene en orden cronologico. El frontend pide
     * paginas crecientes para anteponer mensajes viejos ("cargar anteriores").
     */
    @Transactional(readOnly = true)
    public ChatPageResponse historyPageForUser(JwtPrincipal principal, Long ticketId, int page, int size) {
        Ticket ticket = accessibleTicket(principal, ticketId);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<Message> result = messageRepository.findByTicketIdOrderByCreatedAtDescIdDesc(
                ticket.getId(), PageRequest.of(safePage, safeSize));
        return ChatPageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public List<Long> onlineParticipants(JwtPrincipal principal, Long ticketId) {
        Ticket ticket = accessibleTicket(principal, ticketId);
        return presenceService.onlineUsers(ticket.getTenant().getId());
    }

    /** Presencia con datos reales de usuario (nombre y rol) para mostrar en el chat. */
    @Transactional(readOnly = true)
    public List<OnlineUserResponse> onlineParticipantsDetailed(JwtPrincipal principal, Long ticketId) {
        Ticket ticket = accessibleTicket(principal, ticketId);
        Long tenantId = ticket.getTenant().getId();
        return presenceService.onlineUsers(tenantId).stream()
                .map(userRepository::findById)
                .flatMap(optional -> optional.stream())
                .filter(user -> user.getTenant().getId().equals(tenantId))
                .map(user -> new OnlineUserResponse(user.getId(), user.getFullName(), user.getRole().name()))
                .toList();
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
