package com.chessgame.server.session;

import com.chessgame.server.game.RoomRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public final class SessionLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(SessionLifecycleListener.class);

    private final RoomRegistry roomRegistry;

    public SessionLifecycleListener(RoomRegistry roomRegistry) {
        this.roomRegistry = roomRegistry;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        log.info("websocket connected: session={}", event.getMessage().getHeaders().get("simpSessionId"));
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        log.info("websocket disconnected: session={}", event.getSessionId());
        roomRegistry.leave(event.getSessionId());
    }
}
