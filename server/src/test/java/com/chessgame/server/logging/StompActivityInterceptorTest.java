package com.chessgame.server.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chessgame.server.game.RoomRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompActivityInterceptorTest {

    @Mock
    private ObjectProvider<RoomRegistry> roomRegistryProvider;

    @Mock
    private RoomRegistry roomRegistry;

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger("ACTIVITY")).addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        ((Logger) LoggerFactory.getLogger("ACTIVITY")).detachAppender(appender);
    }

    private Message<byte[]> messageFor(StompCommand command, String destination, String sessionId, String body) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (sessionId != null) {
            accessor.setSessionId(sessionId);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(body.getBytes(StandardCharsets.UTF_8), accessor.getMessageHeaders());
    }

    @Test
    void preSend_returnsTheMessageUnchanged() {
        StompActivityInterceptor interceptor = new StompActivityInterceptor(true, roomRegistryProvider);
        Message<byte[]> message = messageFor(StompCommand.SEND, "/app/move", "session-1", "{}");

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
    }

    @Test
    void preSend_logsInbound_withTheResolvedUsername() {
        when(roomRegistryProvider.getIfAvailable()).thenReturn(roomRegistry);
        when(roomRegistry.usernameForSession("session-1")).thenReturn(Optional.of("alice"));
        StompActivityInterceptor interceptor = new StompActivityInterceptor(true, roomRegistryProvider);
        Message<byte[]> message = messageFor(StompCommand.SEND, "/app/move", "session-1", "{\"fromRow\":6}");

        interceptor.preSend(message, null);

        ILoggingEvent event = appender.list.get(0);
        assertTrue(event.getFormattedMessage().startsWith("IN"));
        assertEquals("alice", event.getArgumentArray()[1]);
        assertEquals("/app/move", event.getArgumentArray()[2]);
    }

    @Test
    void preSend_logsOutbound_whenConstructedForTheOutboundChannel() {
        StompActivityInterceptor interceptor = new StompActivityInterceptor(false, roomRegistryProvider);
        Message<byte[]> message = messageFor(StompCommand.MESSAGE, "/topic/room/ABC123/game", null, "{}");

        interceptor.preSend(message, null);

        assertTrue(appender.list.get(0).getFormattedMessage().startsWith("OUT"));
    }

    @Test
    void preSend_resolvesNoUsername_whenThereIsNoSession() {
        StompActivityInterceptor interceptor = new StompActivityInterceptor(true, roomRegistryProvider);
        Message<byte[]> message = messageFor(StompCommand.CONNECT, null, null, "");

        interceptor.preSend(message, null);

        assertEquals("-", appender.list.get(0).getArgumentArray()[1]);
    }

    @Test
    void preSend_neverThrows_whenTheRoomRegistryLookupFails() {
        when(roomRegistryProvider.getIfAvailable()).thenThrow(new IllegalStateException("boom"));
        StompActivityInterceptor interceptor = new StompActivityInterceptor(true, roomRegistryProvider);
        Message<byte[]> message = messageFor(StompCommand.SEND, "/app/move", "session-1", "{}");

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
    }
}
