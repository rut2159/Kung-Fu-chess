package com.chessgame.server.session;

import com.chessgame.server.service.GameService;
import com.chessgame.server.service.PlayerAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public final class SessionLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(SessionLifecycleListener.class);

    private final PlayerAssignmentService playerAssignmentService;
    private final GameService gameService;

    public SessionLifecycleListener(PlayerAssignmentService playerAssignmentService, GameService gameService) {
        this.playerAssignmentService = playerAssignmentService;
        this.gameService = gameService;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        log.info("websocket connected: session={}", event.getMessage().getHeaders().get("simpSessionId"));
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.info("websocket disconnected: session={}", sessionId);
        playerAssignmentService.disconnect(sessionId)
                .ifPresent(gameService::playerDisconnected);
    }
}
