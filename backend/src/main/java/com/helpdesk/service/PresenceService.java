package com.helpdesk.service;

import com.helpdesk.config.JwtPrincipal;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private final Map<Long, Map<Long, Integer>> onlineByTenant = new ConcurrentHashMap<>();
    private final SimpMessageSendingOperations messagingTemplate;

    public PresenceService(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        handleSession(event.getUser(), true);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        handleSession(event.getUser(), false);
    }

    public List<Long> onlineUsers(Long tenantId) {
        Map<Long, Integer> users = onlineByTenant.get(tenantId);
        if (users == null) {
            return List.of();
        }
        return new ArrayList<>(users.keySet());
    }

    public boolean isOnline(Long tenantId, Long userId) {
        Map<Long, Integer> users = onlineByTenant.get(tenantId);
        return users != null && users.containsKey(userId);
    }

    private synchronized void handleSession(java.security.Principal user, boolean connected) {
        if (!(user instanceof JwtPrincipal principal)) {
            return;
        }
        Map<Long, Integer> users = onlineByTenant
                .computeIfAbsent(principal.getTenantId(), k -> new ConcurrentHashMap<>());

        if (connected) {
            users.merge(principal.getUserId(), 1, Integer::sum);
        } else {
            users.computeIfPresent(principal.getUserId(), (k, count) ->
                    count <= 1 ? null : count - 1);
        }

        List<Long> onlineNow = onlineUsers(principal.getTenantId());
        Object payload = Map.of("online", onlineNow);
        messagingTemplate.convertAndSend(
                "/topic/presence/" + principal.getTenantId(),
                payload);
    }
}
