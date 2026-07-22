package com.chessgame.server.controller;

import com.chessgame.engine.moves.MoveResult;
import com.chessgame.rules.MoveReason;
import com.chessgame.server.dto.JoinCommand;
import com.chessgame.server.dto.JumpCommand;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.dto.MoveRejectedMessage;
import com.chessgame.server.service.GameService;
import com.chessgame.server.service.PlayerAssignmentService;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Successful moves are now broadcast by GameService itself (bus-driven) -
 * see GameServiceTest for that. This controller's own remaining
 * responsibility is sending a rejection notice to the requester only, and
 * resolving join tokens to real usernames (never trusting a client claim).
 */
@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private PlayerAssignmentService playerAssignmentService;

    @Mock
    private SessionTokenService sessionTokenService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private GameController newController() {
        return new GameController(gameService, playerAssignmentService, sessionTokenService, messagingTemplate);
    }

    @Test
    void acceptedMove_doesNotSendARejectionNotice() {
        GameController controller = newController();
        when(gameService.handleMove(any(), anyString())).thenReturn(MoveResult.accepted());

        controller.onMove(new MoveCommand(6, 4, 4, 4), "session-1");

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(MoveRejectedMessage.class));
    }

    @Test
    void rejectedMove_sendsARejectionNoticeWithTheSendersUsername() {
        GameController controller = newController();
        when(gameService.handleMove(any(), anyString()))
                .thenReturn(MoveResult.rejected(MoveReason.ILLEGAL_PIECE_MOVE));
        when(playerAssignmentService.usernameForSession("session-1")).thenReturn(Optional.of("alice"));

        controller.onMove(new MoveCommand(6, 4, 4, 4), "session-1");

        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/errors"),
                eq((Object) new MoveRejectedMessage("alice", "ILLEGAL_PIECE_MOVE")));
    }

    @Test
    void rejectedJump_alsoSendsARejectionNotice() {
        GameController controller = newController();
        when(gameService.handleJump(any(), anyString()))
                .thenReturn(MoveResult.rejected(MoveReason.MOTION_IN_PROGRESS));
        when(playerAssignmentService.usernameForSession("session-1")).thenReturn(Optional.of("bob"));

        controller.onJump(new JumpCommand(4, 4), "session-1");

        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/errors"),
                eq((Object) new MoveRejectedMessage("bob", "MOTION_IN_PROGRESS")));
    }

    @Test
    void join_withAValidToken_resolvesTheRealUsernameAndDelegatesToGameService() {
        GameController controller = newController();
        when(sessionTokenService.resolveUsername("tok-123")).thenReturn(Optional.of("alice"));

        controller.onJoin(new JoinCommand("tok-123"), "session-1");

        verify(gameService, times(1)).join("session-1", "alice");
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void join_withAnUnknownToken_doesNothing_noImpersonationPossible() {
        GameController controller = newController();
        when(sessionTokenService.resolveUsername("made-up-token")).thenReturn(Optional.empty());

        controller.onJoin(new JoinCommand("made-up-token"), "session-1");

        verify(gameService, never()).join(anyString(), anyString());
    }
}
