package com.chessgame.server.service;

import com.chessgame.GameSession;
import com.chessgame.bus.events.GameOverEvent;
import com.chessgame.bus.events.MoveMadeEvent;
import com.chessgame.bus.events.ScoreChangedEvent;
import com.chessgame.engine.moves.MoveResult;
import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.rules.MoveReason;
import com.chessgame.server.dto.GameStateMessage;
import com.chessgame.server.dto.JumpCommand;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.dto.MoveHistoryEntryMessage;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public final class GameService {

    private static final String GAME_STATE_TOPIC = "/topic/game";
    private static final String MOVE_HISTORY_TOPIC = "/topic/moves";

    private static final String STARTING_POSITION =
            "bR bN bB bQ bK bB bN bR\n" +
            "bP bP bP bP bP bP bP bP\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            "wP wP wP wP wP wP wP wP\n" +
            "wR wN wB wQ wK wB wN wR";

    private final GameSession session;
    private final PlayerAssignmentService playerAssignmentService;
    private final RatingService ratingService;
    private final SimpMessagingTemplate messagingTemplate;

    private final ExecutorService gameThread = Executors.newSingleThreadExecutor();

    @Autowired
    public GameService(PlayerAssignmentService playerAssignmentService, RatingService ratingService,
                        SimpMessagingTemplate messagingTemplate) {
        this(playerAssignmentService, ratingService, messagingTemplate,
                new BoardParser().parse(STARTING_POSITION));
    }

    GameService(PlayerAssignmentService playerAssignmentService, RatingService ratingService,
                SimpMessagingTemplate messagingTemplate, Board board) {
        this.playerAssignmentService = playerAssignmentService;
        this.ratingService = ratingService;
        this.messagingTemplate = messagingTemplate;

        this.session = new GameSession(board);
        session.gameEngine.eventBus().subscribe(GameOverEvent.class, this::onGameOver);
        session.gameEngine.eventBus().subscribe(MoveMadeEvent.class, event -> broadcastState());
        session.gameEngine.eventBus().subscribe(MoveMadeEvent.class, this::broadcastMoveHistoryEntry);
        session.gameEngine.eventBus().subscribe(ScoreChangedEvent.class, event -> broadcastState());
    }

    @PreDestroy
    void shutdown() {
        gameThread.shutdownNow();
    }

    public PlayerAssignmentService.Role join(String sessionId, String username) {
        return submit(() -> {
            PlayerAssignmentService.Role role = playerAssignmentService.assign(sessionId, username);
            broadcastState();
            return role;
        });
    }

    public MoveResult handleMove(MoveCommand command, String sessionId) {
        return submit(() -> {
            Position from = new Position(command.fromRow(), command.fromCol());
            MoveResult ownershipCheck = checkOwnership(from, sessionId);
            if (ownershipCheck != null) {
                return ownershipCheck;
            }
            Position to = new Position(command.toRow(), command.toCol());
            return session.gameEngine.requestMove(from, to);
        });
    }

    public MoveResult handleJump(JumpCommand command, String sessionId) {
        return submit(() -> {
            Position position = new Position(command.row(), command.col());
            MoveResult ownershipCheck = checkOwnership(position, sessionId);
            if (ownershipCheck != null) {
                return ownershipCheck;
            }
            MoveResult result = session.gameEngine.requestJump(position);
            if (result.isAccepted()) {
                broadcastState();
            }
            return result;
        });
    }

    public void advanceTimeAndBroadcast(int milliseconds) {
        submit(() -> {
            session.gameEngine.wait(milliseconds);
            broadcastState();
            return null;
        });
    }

    private MoveResult checkOwnership(Position position, String sessionId) {
        if (!playerAssignmentService.bothSeatsFilled()) {
            session.gameEngine.eventBus().publish(
                    new com.chessgame.bus.events.MoveRejectedEvent(MoveReason.WAITING_FOR_OPPONENT));
            return MoveResult.rejected(MoveReason.WAITING_FOR_OPPONENT);
        }

        Piece piece = session.board.pieceAt(position);
        if (piece == null) {
            return null; // let GameEngine give its own real "empty source" rejection
        }

        PlayerAssignmentService.Role role = playerAssignmentService.roleForSession(sessionId);
        boolean ownsThisPiece = (piece.color() == Piece.Color.WHITE && role == PlayerAssignmentService.Role.WHITE)
                || (piece.color() == Piece.Color.BLACK && role == PlayerAssignmentService.Role.BLACK);

        if (ownsThisPiece) {
            return null;
        }
        session.gameEngine.eventBus().publish(
                new com.chessgame.bus.events.MoveRejectedEvent(MoveReason.ILLEGAL_PIECE_MOVE));
        return MoveResult.rejected(MoveReason.ILLEGAL_PIECE_MOVE);
    }

    public GameStateMessage currentState() {
        String white = playerAssignmentService.whiteUsername().orElse(null);
        String black = playerAssignmentService.blackUsername().orElse(null);
        int whiteScore = session.gameEngine.score(Piece.Color.WHITE);
        int blackScore = session.gameEngine.score(Piece.Color.BLACK);
        return GameStateMessage.from(session.gameEngine.snapshot(null), white, black, whiteScore, blackScore);
    }

    private void broadcastState() {
        messagingTemplate.convertAndSend(GAME_STATE_TOPIC, currentState());
    }

    private void broadcastMoveHistoryEntry(MoveMadeEvent event) {
        var record = event.record();
        MoveHistoryEntryMessage entry = new MoveHistoryEntryMessage(
                record.color().name(),
                MoveNotationFormatter.formatMove(record),
                MoveNotationFormatter.formatTime(record.timestamp()));
        messagingTemplate.convertAndSend(MOVE_HISTORY_TOPIC, entry);
    }

    private void onGameOver(GameOverEvent event) {
        broadcastState();
        Piece.Color winner = session.gameEngine.snapshot(null).winner();
        ratingService.applyGameResult(
                playerAssignmentService.whiteUsername().orElse(null),
                playerAssignmentService.blackUsername().orElse(null),
                winner);
    }

    private <T> T submit(Callable<T> task) {
        try {
            return gameThread.submit(task).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the game thread", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Game thread task failed", e.getCause());
        }
    }
}
