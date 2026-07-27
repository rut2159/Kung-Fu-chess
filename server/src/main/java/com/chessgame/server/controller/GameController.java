package com.chessgame.server.controller;

import com.chessgame.engine.moves.MoveResult;
import com.chessgame.server.dto.JoinCommand;
import com.chessgame.server.dto.JoinRejectedMessage;
import com.chessgame.server.dto.JumpCommand;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.dto.MoveRejectedMessage;
import com.chessgame.server.game.GameRoom;
import com.chessgame.server.game.RoomId;
import com.chessgame.server.game.RoomRegistry;
import com.chessgame.server.game.Topics;
import com.chessgame.server.service.SessionTokenService;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class GameController {

    private final RoomRegistry roomRegistry;
    private final SessionTokenService sessionTokenService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameController(RoomRegistry roomRegistry, SessionTokenService sessionTokenService,
                           SimpMessagingTemplate messagingTemplate) {
        this.roomRegistry = roomRegistry;
        this.sessionTokenService = sessionTokenService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/join")
    public void onJoin(JoinCommand command, @Header("simpSessionId") String sessionId) {
        sessionTokenService.resolveUsername(command.token()).ifPresent(username -> {
            RoomRegistry.JoinOutcome outcome = roomRegistry.join(command.roomId(), sessionId, username);
            if (!outcome.isJoined()) {
                messagingTemplate.convertAndSend(Topics.errors(RoomId.normalise(command.roomId())),
                        JoinRejectedMessage.of(username, outcome.status().name(), outcome.occupiedRoomId()));
            }
        });
    }

    @MessageMapping("/move")
    public void onMove(MoveCommand command, @Header("simpSessionId") String sessionId) {
        Optional<GameRoom> room = roomRegistry.roomForSession(sessionId);
        if (room.isEmpty()) {
            return;
        }
        String username = roomRegistry.usernameForSession(sessionId).orElse(null);
        MoveResult result = room.get().handleMove(command, username);
        if (!result.isAccepted()) {
            notifyRejected(room.get(), username, result);
        }
    }

    @MessageMapping("/jump")
    public void onJump(JumpCommand command, @Header("simpSessionId") String sessionId) {
        Optional<GameRoom> room = roomRegistry.roomForSession(sessionId);
        if (room.isEmpty()) {
            return;
        }
        String username = roomRegistry.usernameForSession(sessionId).orElse(null);
        MoveResult result = room.get().handleJump(command, username);
        if (!result.isAccepted()) {
            notifyRejected(room.get(), username, result);
        }
    }

    @MessageMapping("/leave")
    public void onLeave(@Header("simpSessionId") String sessionId) {
        roomRegistry.leave(sessionId);
    }

    private void notifyRejected(GameRoom room, String username, MoveResult result) {
        messagingTemplate.convertAndSend(Topics.errors(room.roomId()),
                new MoveRejectedMessage(username, result.reason().name()));
    }
}
