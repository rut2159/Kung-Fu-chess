package com.chessgame.server.game;

import com.chessgame.GameSession;
import com.chessgame.bus.events.GameOverEvent;
import com.chessgame.bus.events.MoveMadeEvent;
import com.chessgame.bus.events.MoveRejectedEvent;
import com.chessgame.bus.events.ScoreChangedEvent;
import com.chessgame.engine.moves.MoveRecord;
import com.chessgame.engine.moves.MoveResult;
import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.notation.MoveNotation;
import com.chessgame.rules.MoveReason;
import com.chessgame.server.dto.GameStateMessage;
import com.chessgame.server.dto.JumpCommand;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.dto.MoveHistoryEntryMessage;
import com.chessgame.server.service.PlayerAssignmentService;
import com.chessgame.server.service.RatingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class GameRoom {

    private static final Logger log = LoggerFactory.getLogger(GameRoom.class);

    private static final int MAX_QUEUED_TICKS = 20;

    private static final int RESIGN_COUNTDOWN_MS = 20_000;

    private final String roomId;
    private final GameSession session;
    private final PlayerAssignmentService playerAssignmentService;
    private final RatingService ratingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MoveNotation notation;

    private final ExecutorService gameThread;
    private final AtomicInteger queuedTicks = new AtomicInteger();

    private GameStateMessage lastBroadcast;

    private String disconnectedUsername;
    private int resignCountdownMs;

    public GameRoom(String roomId,
                    Board board,
                    PlayerAssignmentService playerAssignmentService,
                    RatingService ratingService,
                    SimpMessagingTemplate messagingTemplate) {
        this.roomId = roomId;
        this.playerAssignmentService = playerAssignmentService;
        this.ratingService = ratingService;
        this.messagingTemplate = messagingTemplate;

        this.session = new GameSession(board);
        this.notation = new MoveNotation(board.height());
        this.gameThread = Executors.newSingleThreadExecutor(
                runnable -> new Thread(runnable, "game-room-" + roomId));

        session.gameEngine.eventBus().subscribe(GameOverEvent.class, this::onGameOver);
        session.gameEngine.eventBus().subscribe(MoveMadeEvent.class, event -> broadcastState());
        session.gameEngine.eventBus().subscribe(MoveMadeEvent.class, this::broadcastMoveHistoryEntry);
        session.gameEngine.eventBus().subscribe(ScoreChangedEvent.class, event -> broadcastState());
    }

    public String roomId() {
        return roomId;
    }

    public void shutdown() {
        gameThread.shutdownNow();
    }

    public PlayerAssignmentService.Role join(String sessionId, String username) {
        return submit(() -> {
            PlayerAssignmentService.Role role = playerAssignmentService.assign(sessionId, username);
            if (username.equals(disconnectedUsername) && !session.gameEngine.isGameOver()) {
                clearResignCountdown();
            }
            broadcastStateToAll();
            return role;
        });
    }


    public void playerDisconnected(String username) {
        submit(() -> {
            PlayerAssignmentService.Role role = playerAssignmentService.roleForUsername(username);
            Piece.Color color = colorOf(role);
            if (color == null || session.gameEngine.isGameOver()) {
                return null;
            }
            disconnectedUsername = username;
            resignCountdownMs = RESIGN_COUNTDOWN_MS;
            log.info("room {}: {} disconnected - abandoning in {} ms unless they return", roomId, username, RESIGN_COUNTDOWN_MS);
            broadcastStateToAll();
            return null;
        });
    }

    private static Piece.Color colorOf(PlayerAssignmentService.Role role) {
        if (role.owns(Piece.Color.WHITE)) return Piece.Color.WHITE;
        if (role.owns(Piece.Color.BLACK)) return Piece.Color.BLACK;
        return null;
    }

    private void clearResignCountdown() {
        if (disconnectedUsername != null) {
            log.info("room {}: {} reconnected - countdown cancelled", roomId, disconnectedUsername);
        }
        disconnectedUsername = null;
        resignCountdownMs = 0;
    }


    private void advanceTime(int milliseconds) {
        if (session.gameEngine.isGameOver()) {
            return;
        }
        if (disconnectedUsername != null) {
            advanceResignCountdown(milliseconds);
            return;
        }
        session.gameEngine.wait(milliseconds);
    }

    private void advanceResignCountdown(int milliseconds) {
        resignCountdownMs -= milliseconds;
        if (resignCountdownMs > 0) {
            return;
        }
        log.info("room {}: {} did not return - abandoning the game", roomId, disconnectedUsername);
        resignCountdownMs = 0;
        session.gameEngine.abandon();
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
            advanceTime(milliseconds);
            broadcastState();
            return null;
        });
    }

     public void tick(int milliseconds) {
        if (queuedTicks.get() >= MAX_QUEUED_TICKS) {
            log.warn("room {} is falling behind - dropping a tick", roomId);
            return;
        }
        queuedTicks.incrementAndGet();
        gameThread.execute(() -> {
            try {
                advanceTime(milliseconds);
                broadcastState();
            } catch (RuntimeException e) {
                log.error("game tick failed in room {} - the clock continues", roomId, e);
            } finally {
                queuedTicks.decrementAndGet();
            }
        });
    }

    private Optional<MoveResult> ownershipRejection(Position position, String sessionId) {
        if (!playerAssignmentService.bothSeatsFilled()) {
            return Optional.of(reject(MoveReason.WAITING_FOR_OPPONENT));
        }

        if (disconnectedUsername != null) {
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
        boolean counting = disconnectedUsername != null && !session.gameEngine.isGameOver();
        Integer resignInSeconds = counting ? Math.max(0, (resignCountdownMs + 999) / 1000) : null;
        return GameStateMessage.from(session.gameEngine.snapshot(null), white, black,
                whiteScore, blackScore, disconnectedUsername, resignInSeconds);
    }
    private void broadcastState() {
        GameStateMessage state = currentState();
        if (state.equals(lastBroadcast)) {
            return;
        }
        lastBroadcast = state;
        messagingTemplate.convertAndSend(Topics.GAME_STATE, state);
    }

    private void broadcastStateToAll() {
        lastBroadcast = null;
        broadcastState();
    }

    private void broadcastMoveHistoryEntry(MoveMadeEvent event) {
        messagingTemplate.convertAndSend(Topics.MOVE_HISTORY, toEntry(event.record()));
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
