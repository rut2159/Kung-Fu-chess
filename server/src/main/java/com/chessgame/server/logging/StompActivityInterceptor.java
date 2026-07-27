package com.chessgame.server.logging;

import com.chessgame.server.game.RoomRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

import java.nio.charset.StandardCharsets;

public final class StompActivityInterceptor implements ChannelInterceptor {

    private final boolean inbound;
    private final ObjectProvider<RoomRegistry> roomRegistryProvider;

    public StompActivityInterceptor(boolean inbound, ObjectProvider<RoomRegistry> roomRegistryProvider) {
        this.inbound = inbound;
        this.roomRegistryProvider = roomRegistryProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        try {
            record(message);
        } catch (RuntimeException ignored) {
            return message;
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

        String username = resolveUsername(sessionId);
        String body = bodyOf(message);

        if (inbound) {
            ActivityLog.inbound(sessionId, username, destination, body);
        } else {
            ActivityLog.outbound(sessionId, username, destination, body);
        }
    }

    private String resolveUsername(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        RoomRegistry registry = roomRegistryProvider.getIfAvailable();
        if (registry == null) {
            return null;
        }
        return registry.usernameForSession(sessionId).orElse(null);
    }

    private static String bodyOf(Message<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(payload);
    }
}
