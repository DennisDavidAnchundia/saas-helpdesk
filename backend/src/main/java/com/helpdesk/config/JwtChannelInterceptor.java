package com.helpdesk.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    public JwtChannelInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");

            if (header == null || !header.startsWith("Bearer ")
                    || !jwtProvider.validateToken(header.substring(7))) {
                throw new MessagingException("Token faltante o invalido");
            }

            String token = header.substring(7);
            JwtPrincipal principal = new JwtPrincipal(
                    jwtProvider.getUserIdFromToken(token),
                    jwtProvider.getTenantIdFromToken(token),
                    jwtProvider.getEmailFromToken(token),
                    jwtProvider.getRoleFromToken(token));

            accessor.setUser(principal);
        }
        return message;
    }
}
