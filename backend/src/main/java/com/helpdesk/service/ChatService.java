package com.helpdesk.service;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.ChatMessageResponse;
import com.helpdesk.dto.SendChatMessageRequest;
import com.helpdesk.model.Message;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.repository.MessageRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public ChatService(MessageRepository messageRepository,
                       TicketRepository ticketRepository,
                       UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
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
