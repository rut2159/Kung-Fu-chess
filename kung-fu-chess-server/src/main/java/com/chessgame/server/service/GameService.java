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

/**
 * Owns the one game currently running on this server.
 *
 * This is deliberately a single global game for now - matching "Single-process
 * server" from the requirements (one board, up to two players). Rooms/multiple
 * concurrent games are a later step (group D): at that point this class becomes
 * a lookup by room id, and each room gets its own gameThread below.
 *
 * Every state-mutating operation (join, move, jump, the periodic time tick)
 * is serialized through a single-threaded executor - without this, the
 * @Scheduled ticker thread and STOMP message-handling threads could touch the
 * same GameEngine/Board concurrently with no coordination at all.
 */
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

    /** Visible for tests that need a specific, minimal board instead of the full starting position. */
    GameService(PlayerAssignmentService playerAssignmentService, RatingService ratingService,
                SimpMessagingTemplate messagingTemplate, Board board) {
        this.playerAssignmentService = playerAssignmentService;
        this.ratingService = ratingService;
        this.messagingTemplate = messagingTemplate;

        this.session = new GameSession(board);

        // Broadcast is wired as a bus subscriber here - same standing as the
        // logging/sound subscribers - rather than being called imperatively
        // from the controller after every command. GameOver additionally
        // triggers the ELO update.
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
                // core doesn't publish a bus event for jump yet (a known,
                // pre-existing gap - see the "jump sound" discussion earlier),
                // so this one broadcast stays manual rather than bus-driven.
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

    /**
     * Enforces "support only 2 players: first joined is white, second is
     * black" - a viewer, or a player trying to move the opponent's piece, is
     * rejected here, before the request ever reaches the real GameEngine.
     * Returns null when the check passes (caller proceeds normally).
     */
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
