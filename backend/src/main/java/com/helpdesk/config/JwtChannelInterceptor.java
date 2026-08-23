package com.helpdesk.config;

import com.helpdesk.model.User;
import com.helpdesk.repository.UserRepository;
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
    private final UserRepository userRepository;

    public JwtChannelInterceptor(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
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
            Long userId = jwtProvider.getUserIdFromToken(token);

            // Igual que en el filtro HTTP: usuario desactivado no conecta
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isActive()) {
                throw new MessagingException("Cuenta desactivada");
            }

            JwtPrincipal principal = new JwtPrincipal(
                    userId,
                    jwtProvider.getTenantIdFromToken(token),
                    jwtProvider.getEmailFromToken(token),
                    jwtProvider.getRoleFromToken(token));

            accessor.setUser(principal);
        }
        return message;
    }
}
