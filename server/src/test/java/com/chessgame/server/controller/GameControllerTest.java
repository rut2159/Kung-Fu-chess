package com.chessgame.server.controller;

import com.chessgame.engine.moves.MoveResult;
import com.chessgame.rules.MoveReason;
import com.chessgame.server.dto.JoinCommand;
import com.chessgame.server.dto.JoinRejectedMessage;
import com.chessgame.server.dto.JumpCommand;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.dto.MoveRejectedMessage;
import com.chessgame.server.game.GameRoom;
import com.chessgame.server.game.RoomRegistry;
import com.chessgame.server.game.Seats;
import com.chessgame.server.game.Topics;
import com.chessgame.server.service.SessionTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    private static final String ROOM_ID = "K7F2QX";

    @Mock
    private RoomRegistry roomRegistry;

    @Mock
    private GameRoom room;

    @Mock
    private SessionTokenService sessionTokenService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private GameController newController() {
        return new GameController(roomRegistry, sessionTokenService, messagingTemplate);
    }

    private void sessionIsInARoom() {
        when(roomRegistry.roomForSession("session-1")).thenReturn(Optional.of(room));
        when(roomRegistry.usernameForSession("session-1")).thenReturn(Optional.of("alice"));
        when(room.roomId()).thenReturn(ROOM_ID);
    }

    @Test
    void acceptedMoveDoesNotSendARejectionNotice() {
        GameController controller = newController();
        when(roomRegistry.roomForSession("session-1")).thenReturn(Optional.of(room));
        when(roomRegistry.usernameForSession("session-1")).thenReturn(Optional.of("alice"));
        when(room.handleMove(any(), anyString())).thenReturn(MoveResult.accepted());

        controller.onMove(new MoveCommand(6, 4, 4, 4), "session-1");

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(MoveRejectedMessage.class));
    }

    @Test
    void rejectedMoveIsReportedOnTheRoomErrorTopic() {
        GameController controller = newController();
        sessionIsInARoom();
        when(room.handleMove(any(), anyString()))
                .thenReturn(MoveResult.rejected(MoveReason.ILLEGAL_PIECE_MOVE));

        controller.onMove(new MoveCommand(6, 4, 4, 4), "session-1");

        verify(messagingTemplate).convertAndSend(eq(Topics.errors(ROOM_ID)),
                any(MoveRejectedMessage.class));
    }

    @Test
    void rejectedJumpIsReportedOnTheRoomErrorTopic() {
        GameController controller = newController();
        sessionIsInARoom();
        when(room.handleJump(any(), anyString()))
                .thenReturn(MoveResult.rejected(MoveReason.ILLEGAL_PIECE_MOVE));

        controller.onJump(new JumpCommand(6, 4), "session-1");

        verify(messagingTemplate).convertAndSend(eq(Topics.errors(ROOM_ID)),
                any(MoveRejectedMessage.class));
    }

    @Test
    void aMoveFromASessionWithNoRoomIsIgnored() {
        GameController controller = newController();
        when(roomRegistry.roomForSession("session-1")).thenReturn(Optional.empty());

        controller.onMove(new MoveCommand(6, 4, 4, 4), "session-1");

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void joinResolvesTheUsernameFromTheTokenAndNeverFromTheClient() {
        GameController controller = newController();
        when(sessionTokenService.resolveUsername("good-token")).thenReturn(Optional.of("alice"));
        when(roomRegistry.join(ROOM_ID, "session-1", "alice"))
                .thenReturn(new RoomRegistry.JoinOutcome(
                        RoomRegistry.JoinOutcome.Status.JOINED, room, Seats.Role.WHITE, null));

        controller.onJoin(new JoinCommand("good-token", ROOM_ID), "session-1");

        verify(roomRegistry).join(ROOM_ID, "session-1", "alice");
    }

    @Test
    void joinWithAnUnknownTokenIsIgnored() {
        GameController controller = newController();
        when(sessionTokenService.resolveUsername("bad-token")).thenReturn(Optional.empty());

        controller.onJoin(new JoinCommand("bad-token", ROOM_ID), "session-1");

        verify(roomRegistry, never()).join(anyString(), anyString(), anyString());
    }

    @Test
    void aRejectedJoinIsReportedBackToTheClient() {
        GameController controller = newController();
        when(sessionTokenService.resolveUsername("good-token")).thenReturn(Optional.of("alice"));
        when(roomRegistry.join("ZZZZZZ", "session-1", "alice"))
                .thenReturn(new RoomRegistry.JoinOutcome(
                        RoomRegistry.JoinOutcome.Status.ROOM_NOT_FOUND, null, null, null));

        controller.onJoin(new JoinCommand("good-token", "ZZZZZZ"), "session-1");

        verify(messagingTemplate).convertAndSend(eq(Topics.errors("ZZZZZZ")),
                any(JoinRejectedMessage.class));
    }

    @Test
    void aRejectedJoinNamesThePlayerItConcerns() {
        GameController controller = newController();
        when(sessionTokenService.resolveUsername("good-token")).thenReturn(Optional.of("alice"));
        when(roomRegistry.join(ROOM_ID, "session-1", "alice"))
                .thenReturn(new RoomRegistry.JoinOutcome(
                        RoomRegistry.JoinOutcome.Status.ALREADY_IN_ANOTHER_ROOM, null, null, "OTHER1"));

        controller.onJoin(new JoinCommand("good-token", ROOM_ID), "session-1");

        // The errors topic is shared, so every other client in ROOM_ID also
        // receives this. Without a name they would all react to Alice's failure.
        verify(messagingTemplate).convertAndSend(eq(Topics.errors(ROOM_ID)),
                eq(JoinRejectedMessage.of("alice", "ALREADY_IN_ANOTHER_ROOM", "OTHER1")));
    }

    @Test
    void anExplicitLeaveReleasesTheSessionWithoutWaitingForTheDisconnectEvent() {
        GameController controller = newController();

        controller.onLeave("session-1");

        verify(roomRegistry).leave("session-1");
    }
}
