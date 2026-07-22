package com.chessgame.server.controller;

import com.chessgame.engine.moves.MoveResult;
import com.chessgame.server.dto.JoinCommand;
import com.chessgame.server.dto.JumpCommand;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.dto.MoveRejectedMessage;
import com.chessgame.server.service.GameService;
import com.chessgame.server.service.PlayerAssignmentService;
import com.chessgame.server.service.SessionTokenService;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Successful moves/jumps/joins are broadcast by GameService itself (bus-driven
 * for moves/score/game-over, direct for jump - see GameService). This
 * controller's only remaining messaging responsibility is telling the
 * REQUESTING client, specifically, when their own command was rejected -
 * that's a reply to one sender, not a fact the whole room needs to know,
 * so it doesn't belong on the game-state broadcast or the event bus.
 */
@Controller
public class GameController {

    private static final String ERRORS_TOPIC = "/topic/errors";

    private final GameService gameService;
    private final PlayerAssignmentService playerAssignmentService;
    private final SessionTokenService sessionTokenService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameController(GameService gameService, PlayerAssignmentService playerAssignmentService,
                           SessionTokenService sessionTokenService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.playerAssignmentService = playerAssignmentService;
        this.sessionTokenService = sessionTokenService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * The username is never taken from what the client claims - it's looked
     * up server-side from the token issued at /api/login. An invalid/unknown
     * token (e.g. someone hand-editing the URL) is silently ignored: no role
     * is assigned, so that connection just stays a viewer.
     */
    @MessageMapping("/join")
    public void onJoin(JoinCommand command, @Header("simpSessionId") String sessionId) {
        sessionTokenService.resolveUsername(command.token())
                .ifPresent(username -> gameService.join(sessionId, username));
    }

    @MessageMapping("/move")
    public void onMove(MoveCommand command, @Header("simpSessionId") String sessionId) {
        MoveResult result = gameService.handleMove(command, sessionId);
        if (!result.isAccepted()) {
            notifyRejected(sessionId, result);
        }
    }

    /**
     * Mirrors the desktop's double-click "jump" - resets an idle piece's own
     * cooldown in place without moving it (see Controller.jump / GameEngine.requestJump).
     */
    @MessageMapping("/jump")
    public void onJump(JumpCommand command, @Header("simpSessionId") String sessionId) {
        MoveResult result = gameService.handleJump(command, sessionId);
        if (!result.isAccepted()) {
            notifyRejected(sessionId, result);
        }
    }

    private void notifyRejected(String sessionId, MoveResult result) {
        String username = playerAssignmentService.usernameForSession(sessionId).orElse(null);
        messagingTemplate.convertAndSend(ERRORS_TOPIC,
                new MoveRejectedMessage(username, result.reason().name()));
    }
}
