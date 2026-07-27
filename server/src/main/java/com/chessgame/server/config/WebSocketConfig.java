package com.chessgame.server.config;

import com.chessgame.server.logging.StompActivityInterceptor;
import com.chessgame.server.service.PlayerAssignmentService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final PlayerAssignmentService playerAssignmentService;

    public WebSocketConfig(PlayerAssignmentService playerAssignmentService) {
        this.playerAssignmentService = playerAssignmentService;
    }

    /**
     * Every frame from the browser passes through here, so one interceptor
     * covers all current and future client commands.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompActivityInterceptor(true, playerAssignmentService));
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompActivityInterceptor(false, playerAssignmentService));
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Spring defaults to a 512KB outbound buffer per session, and closes the
     * session the moment it is exceeded:
     *
     *   Terminating 'WebSocketServerSockJsSession[...]':
     *   Buffer size 525402 bytes exceeds the allowed limit 524288
     *
     * That is precisely what happened here: the client stalled briefly - a
     * background tab, a GC pause, a network hiccup - messages piled up, and the
     * server dropped it. With no client-side reconnect, the board simply froze.
     *
     * A larger buffer buys tolerance for brief stalls, but it is not the real
     * fix. That lives in the game room, which no longer broadcasts unchanged state.
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setSendBufferSizeLimit(2 * 1024 * 1024);
        registration.setSendTimeLimit(20 * 1000);
        registration.setMessageSizeLimit(256 * 1024);
    }
}
