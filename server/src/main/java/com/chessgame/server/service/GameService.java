package com.chessgame.server.service;

import com.chessgame.GameSession;
import com.chessgame.bus.events.GameOverEvent;
import com.chessgame.bus.events.MoveMadeEvent;
import com.chessgame.bus.events.MoveRejectedEvent;
import com.chessgame.bus.events.ScoreChangedEvent;
import com.chessgame.engine.moves.MoveRecord;
import com.chessgame.engine.moves.MoveResult;
import com.chessgame.io.StandardBoard;
import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.notation.MoveNotation;
import com.chessgame.rules.MoveReason;
import com.chessgame.server.dto.GameStateMessage;
import com.chessgame.server.dto.JumpCommand;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.dto.MoveHistoryEntryMessage;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public final class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private static final String GAME_STATE_TOPIC = "/topic/game";
    private static final String MOVE_HISTORY_TOPIC = "/topic/moves";

    private static final int MAX_QUEUED_TICKS = 20;

    private final GameSession session;
    private final PlayerAssignmentService playerAssignmentService;
    private final RatingService ratingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MoveNotation notation;

    private final ExecutorService gameThread = Executors.newSingleThreadExecutor();
    private final AtomicInteger queuedTicks = new AtomicInteger();

    private GameStateMessage lastBroadcast;

    @Autowired
    public GameService(PlayerAssignmentService playerAssignmentService, RatingService ratingService,
                       SimpMessagingTemplate messagingTemplate) {
        this(playerAssignmentService, ratingService, messagingTemplate, StandardBoard.create());
    }

    GameService(PlayerAssignmentService playerAssignmentService, RatingService ratingService,
                SimpMessagingTemplate messagingTemplate, Board board) {
        this.playerAssignmentService = playerAssignmentService;
        this.ratingService = ratingService;
        this.messagingTemplate = messagingTemplate;

        this.session = new GameSession(board);
        this.notation = new MoveNotation(board.height());
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
            broadcastStateToAll();
            return role;
        });
    }

    public MoveResult handleMove(MoveCommand command, String sessionId) {
        return submit(() -> {
            Position from = new Position(command.fromRow(), command.fromCol());
            Optional<MoveResult> rejection = ownershipRejection(from, sessionId);
            if (rejection.isPresent()) {
                return rejection.get();
            }
            Position to = new Position(command.toRow(), command.toCol());
            return session.gameEngine.requestMove(from, to);
        });
    }

    public MoveResult handleJump(JumpCommand command, String sessionId) {
        return submit(() -> {
            Position position = new Position(command.row(), command.col());
            Optional<MoveResult> rejection = ownershipRejection(position, sessionId);
            if (rejection.isPresent()) {
                return rejection.get();
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

    public void tick(int milliseconds) {
        if (queuedTicks.get() >= MAX_QUEUED_TICKS) {
            log.warn("game thread is falling behind - dropping a tick");
            return;
        }
        queuedTicks.incrementAndGet();
        gameThread.execute(() -> {
            try {
                session.gameEngine.wait(milliseconds);
                broadcastState();
            } catch (RuntimeException e) {
                log.error("game tick failed - the clock continues", e);
            } finally {
                queuedTicks.decrementAndGet();
            }
        });
    }

    private Optional<MoveResult> ownershipRejection(Position position, String sessionId) {
        if (!playerAssignmentService.bothSeatsFilled()) {
            return Optional.of(reject(MoveReason.WAITING_FOR_OPPONENT));
        }

        Piece piece = session.board.pieceAt(position);
        if (piece == null) {
            return Optional.empty();
        }

        PlayerAssignmentService.Role role = playerAssignmentService.roleForSession(sessionId);
        if (role.owns(piece.color())) {
            return Optional.empty();
        }
        return Optional.of(reject(MoveReason.ILLEGAL_PIECE_MOVE));
    }

    private MoveResult reject(MoveReason reason) {
        session.gameEngine.eventBus().publish(new MoveRejectedEvent(reason));
        return MoveResult.rejected(reason);
    }

    public GameStateMessage currentState() {
        String white = playerAssignmentService.whiteUsername().orElse(null);
        String black = playerAssignmentService.blackUsername().orElse(null);
        int whiteScore = session.gameEngine.score(Piece.Color.WHITE);
        int blackScore = session.gameEngine.score(Piece.Color.BLACK);
        return GameStateMessage.from(session.gameEngine.snapshot(null), white, black, whiteScore, blackScore);
    }

    private void broadcastState() {
        GameStateMessage state = currentState();
        if (state.equals(lastBroadcast)) {
            return;
        }
        lastBroadcast = state;
        messagingTemplate.convertAndSend(GAME_STATE_TOPIC, state);
    }

    private void broadcastStateToAll() {
        lastBroadcast = null;
        broadcastState();
    }

    private void broadcastMoveHistoryEntry(MoveMadeEvent event) {
        messagingTemplate.convertAndSend(MOVE_HISTORY_TOPIC, toEntry(event.record()));
    }

    private MoveHistoryEntryMessage toEntry(MoveRecord record) {
        return new MoveHistoryEntryMessage(
                record.color().name(),
                notation.formatMove(record),
                MoveNotation.formatTime(record.timestamp()),
                record.timestamp());
    }

    public List<MoveHistoryEntryMessage> moveHistory() {
        return submit(() -> session.gameEngine.moveHistory().stream()
                .map(this::toEntry)
                .toList());
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