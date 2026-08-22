package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.ChatMessageResponse;
import com.helpdesk.dto.SendChatMessageRequest;
import com.helpdesk.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatController {

    private final ChatService chatService;
    private final SimpMessageSendingOperations messagingTemplate;

    public ChatController(ChatService chatService,
                          SimpMessageSendingOperations messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/{ticketId}")
    public void handleChat(@DestinationVariable Long ticketId,
                           @Payload @Valid SendChatMessageRequest request,
                           Principal principal) {
        if (!(principal instanceof JwtPrincipal jwtPrincipal)) {
            return;
        }
        ChatMessageResponse response = chatService.send(jwtPrincipal, ticketId, request);
        messagingTemplate.convertAndSend("/topic/tickets/" + ticketId, response);
    }
}
