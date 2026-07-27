package com.chessgame.server.logging;

import com.chessgame.server.service.PlayerAssignmentService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

import java.nio.charset.StandardCharsets;

public final class StompActivityInterceptor implements ChannelInterceptor {

    private final boolean inbound;
    private final PlayerAssignmentService playerAssignmentService;

    public StompActivityInterceptor(boolean inbound, PlayerAssignmentService playerAssignmentService) {
        this.inbound = inbound;
        this.playerAssignmentService = playerAssignmentService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        try {
            record(message);
        } catch (RuntimeException ignored) {
            // Deliberately swallowed - see above.
        }
        return message;
    }

    private void record(Message<?> message) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();

        if (destination == null) {
            destination = accessor.getCommand() == null ? "-" : accessor.getCommand().name();
        }

        String username = sessionId == null
                ? null
                : playerAssignmentService.usernameForSession(sessionId).orElse(null);

        String body = bodyOf(message);

        if (inbound) {
            ActivityLog.inbound(sessionId, username, destination, body);
        } else {
            ActivityLog.outbound(sessionId, username, destination, body);
        }
    }

    private static String bodyOf(Message<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(payload);
    }
}
