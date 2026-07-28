package com.chessgame.server.session;

import com.chessgame.server.game.RoomRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SessionLifecycleListenerTest {

    @Mock
    private RoomRegistry roomRegistry;

    private SessionLifecycleListener newListener() {
        return new SessionLifecycleListener(roomRegistry);
    }

    private SessionDisconnectEvent disconnectEvent(String sessionId) {
        return new SessionDisconnectEvent(this, new GenericMessage<>(new byte[0]), sessionId, CloseStatus.NORMAL);
    }

    @Test
    void onDisconnect_releasesTheSessionsRoom() {
        SessionLifecycleListener listener = newListener();

        listener.onDisconnect(disconnectEvent("session-1"));

        verify(roomRegistry).leave("session-1");
    }

    @Test
    void onDisconnect_doesNotPropagate_whenRoomCleanupFails() {
        SessionLifecycleListener listener = newListener();
        doThrow(new IllegalStateException("boom")).when(roomRegistry).leave("session-1");

        listener.onDisconnect(disconnectEvent("session-1"));

        verify(roomRegistry).leave("session-1");
    }

    @Test
    void onConnected_doesNotTouchTheRoomRegistry() {
        SessionLifecycleListener listener = newListener();

        listener.onConnected(new SessionConnectedEvent(this, new GenericMessage<>(new byte[0])));

        verifyNoMoreRoomRegistryInteractions();
    }

    private void verifyNoMoreRoomRegistryInteractions() {
        org.mockito.Mockito.verifyNoInteractions(roomRegistry);
    }
}
