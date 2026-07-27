package com.chessgame.server.service;

import com.chessgame.engine.moves.MoveResult;
import com.chessgame.io.StandardBoard;
import com.chessgame.model.Board;
import com.chessgame.server.dto.JumpCommand;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.dto.MoveHistoryEntryMessage;
import com.chessgame.server.game.GameRoom;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public final class GameService {

    private static final String DEFAULT_ROOM_ID = "main";

    private final GameRoom room;

    @Autowired
    public GameService(PlayerAssignmentService playerAssignmentService, RatingService ratingService,
                       SimpMessagingTemplate messagingTemplate) {
        this(playerAssignmentService, ratingService, messagingTemplate, StandardBoard.create());
    }

    GameService(PlayerAssignmentService playerAssignmentService, RatingService ratingService,
                SimpMessagingTemplate messagingTemplate, Board board) {
        this.room = new GameRoom(DEFAULT_ROOM_ID, board, playerAssignmentService,
                ratingService, messagingTemplate);
    }

    @PreDestroy
    void shutdown() {
        room.shutdown();
    }

    public PlayerAssignmentService.Role join(String sessionId, String username) {
        return room.join(sessionId, username);
    }

    public void playerDisconnected(String username) {
        room.playerDisconnected(username);
    }

    public MoveResult handleMove(MoveCommand command, String sessionId) {
        return room.handleMove(command, sessionId);
    }

    public MoveResult handleJump(JumpCommand command, String sessionId) {
        return room.handleJump(command, sessionId);
    }

    public void advanceTimeAndBroadcast(int milliseconds) {
        room.advanceTimeAndBroadcast(milliseconds);
    }

    public void tick(int milliseconds) {
        room.tick(milliseconds);
    }

    public List<MoveHistoryEntryMessage> moveHistory() {
        return room.moveHistory();
    }
}
