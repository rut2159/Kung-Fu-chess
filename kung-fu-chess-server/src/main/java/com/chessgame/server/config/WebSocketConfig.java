package com.chessgame.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The single URL a client connects to in order to open the WebSocket
        // handshake. withSockJS() adds a fallback for browsers/networks that
        // block raw WebSocket connections.
        //
        // setAllowedOriginPatterns("*") is needed because test-client.html is
        // opened directly from disk (file://), which browsers treat as a
        // different origin than http://localhost:8080 - without this, Spring
        // silently rejects the handshake as a CORS violation.
        // TODO before any real deployment: replace "*" with the actual
        // origin(s) of the real client (e.g. "http://localhost:3000").
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Messages the SERVER sends to clients go out on destinations starting
        // with /topic (e.g. /topic/game/room-42). enableSimpleBroker keeps the
        // list of subscribers and routes messages in-memory - no external
        // message broker (like RabbitMQ) needed for a single-process server.
        registry.enableSimpleBroker("/topic");

        // Messages a CLIENT sends to the server must be prefixed with /app
        // (e.g. client sends to /app/move). Spring strips the /app prefix and
        // routes what's left (/move) to a @MessageMapping("/move") method.
        registry.setApplicationDestinationPrefixes("/app");
    }
}
