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
import com.chessgame.server.repository.GameRepository;
import com.chessgame.server.repository.MoveHistoryRepository;
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

public class GameRoom {

    private static final Logger log = LoggerFactory.getLogger(GameRoom.class);

    private static final int MAX_QUEUED_TICKS = 20;
    private static final int ABANDON_COUNTDOWN_MS = 20_000;

    private final String roomId;
    private final GameSession session;
    private final Seats seats = new Seats();
    private final RatingService ratingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MoveNotation notation;
    private final GameRepository gameRepository;
    private final MoveHistoryRepository moveHistoryRepository;

    private final ExecutorService gameThread;
    private final AtomicInteger queuedTicks = new AtomicInteger();

    private GameStateMessage lastBroadcast;
    private String absentUsername;
    private int countdownMs;

    private Piece.Color forfeitWinner;
    private Long gameId;
    private int moveSeq;

    public GameRoom(String roomId,
                    Board board,
                    RatingService ratingService,
                    SimpMessagingTemplate messagingTemplate,
                    GameRepository gameRepository,
                    MoveHistoryRepository moveHistoryRepository) {
        this.roomId = roomId;
        this.ratingService = ratingService;
        this.messagingTemplate = messagingTemplate;
        this.gameRepository = gameRepository;
        this.moveHistoryRepository = moveHistoryRepository;

        this.session = new GameSession(board);
        this.notation = new MoveNotation(board.height());
        this.gameThread = Executors.newSingleThreadExecutor(
                runnable -> new Thread(runnable, "game-room-" + roomId));

        session.gameEngine.eventBus().subscribe(GameOverEvent.class, this::onGameOver);
        session.gameEngine.eventBus().subscribe(MoveMadeEvent.class, event -> broadcastState());
        session.gameEngine.eventBus().subscribe(MoveMadeEvent.class, this::broadcastMoveHistoryEntry);
        session.gameEngine.eventBus().subscribe(MoveMadeEvent.class, this::persistMoveHistoryEntry);
        session.gameEngine.eventBus().subscribe(ScoreChangedEvent.class, event -> broadcastState());
    }

    public String roomId() {
        return roomId;
    }

    public void shutdown() {
        gameThread.shutdownNow();
    }

    public Seats.Role join(String username) {
        return submit(() -> {
            Seats.Role role = seats.take(username);
            if (username.equals(absentUsername)) {
                clearCountdown();
            }
            if (gameId == null && seats.bothSeatsFilled()) {
                gameId = gameRepository.startGame(roomId,
                        seats.whiteUsername().orElseThrow(), seats.blackUsername().orElseThrow());
            }
            broadcastStateToAll();
            return role;
        });
    }

    public Seats.Role roleOf(String username) {
        return seats.roleOf(username);
    }

    public boolean isSeated(String username) {
        return seats.roleOf(username) != Seats.Role.VIEWER;
    }

    public Optional<String> whiteUsername() {
        return seats.whiteUsername();
    }

    public Optional<String> blackUsername() {
        return seats.blackUsername();
    }

    public void playerLeft(String username) {
        submit(() -> {
            if (!isSeated(username) || session.gameEngine.isGameOver()) {
                return null;
            }
            // Nobody to forfeit to yet. Counting down here would kill the room
            // under the next person to walk in with the code: they would take
            // the empty seat and the game would end seconds later without them
            // ever having moved. Hand the seat back instead.
            if (!seats.bothSeatsFilled()) {
                seats.release(username);
                log.info("room {}: {} left before the game started, seat released", roomId, username);
                broadcastStateToAll();
                return null;
            }
            absentUsername = username;
            countdownMs = ABANDON_COUNTDOWN_MS;
            log.info("room {}: {} left, forfeiting in {} ms unless they return",
                    roomId, username, ABANDON_COUNTDOWN_MS);
            broadcastStateToAll();
            return null;
        });
    }

    private void clearCountdown() {
        if (absentUsername != null) {
            log.info("room {}: {} returned, countdown cancelled", roomId, absentUsername);
        }
        absentUsername = null;
        countdownMs = 0;
    }

    private void advanceTime(int milliseconds) {
        if (session.gameEngine.isGameOver()) {
            return;
        }
        if (absentUsername != null) {
            advanceCountdown(milliseconds);
            return;
        }
        session.gameEngine.wait(milliseconds);
    }

    private void advanceCountdown(int milliseconds) {
        countdownMs -= milliseconds;
        if (countdownMs > 0) {
            return;
        }
        log.info("room {}: {} did not return, forfeiting the game", roomId, absentUsername);
        countdownMs = 0;
        forfeitWinner = opponentColourOf(absentUsername);
        session.gameEngine.abandon();
    }

    private Piece.Color opponentColourOf(String username) {
        return switch (seats.roleOf(username)) {
            case WHITE -> Piece.Color.BLACK;
            case BLACK -> Piece.Color.WHITE;
            case VIEWER -> null;
        };
    }

    /**
     * The board's verdict, falling back to the forfeit. Only ever consulted
     * once the game is over, so a forfeit can never leak into a live game.
     */
    private Piece.Color decideWinner() {
        if (!session.gameEngine.isGameOver()) {
            return null;
        }
        Piece.Color onTheBoard = session.gameEngine.snapshot(null).winner();
        return onTheBoard != null ? onTheBoard : forfeitWinner;
    }

    public MoveResult handleMove(MoveCommand command, String username) {
        return submit(() -> {
            Position from = new Position(command.fromRow(), command.fromCol());
            Optional<MoveResult> rejection = rejectionFor(from, username);
            if (rejection.isPresent()) {
                return rejection.get();
            }
            Position to = new Position(command.toRow(), command.toCol());
            return session.gameEngine.requestMove(from, to);
        });
    }

    public MoveResult handleJump(JumpCommand command, String username) {
        return submit(() -> {
            Position position = new Position(command.row(), command.col());
            Optional<MoveResult> rejection = rejectionFor(position, username);
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
            log.warn("room {} is falling behind, dropping a tick", roomId);
            return;
        }
        queuedTicks.incrementAndGet();
        gameThread.execute(() -> {
            try {
                advanceTime(milliseconds);
                broadcastState();
            } catch (RuntimeException e) {
                log.error("game tick failed in room {}, the clock continues", roomId, e);
            } finally {
                queuedTicks.decrementAndGet();
            }
        });
    }

    private Optional<MoveResult> rejectionFor(Position position, String username) {
        if (!seats.bothSeatsFilled()) {
            return Optional.of(reject(MoveReason.WAITING_FOR_OPPONENT));
        }
        if (absentUsername != null) {
            return Optional.of(reject(MoveReason.WAITING_FOR_OPPONENT));
        }

        Piece piece = session.board.pieceAt(position);
        if (piece == null) {
            return Optional.empty();
        }
        if (seats.roleOf(username).owns(piece.color())) {
            return Optional.empty();
        }
        return Optional.of(reject(MoveReason.ILLEGAL_PIECE_MOVE));
    }

    private MoveResult reject(MoveReason reason) {
        session.gameEngine.eventBus().publish(new MoveRejectedEvent(reason));
        return MoveResult.rejected(reason);
    }
    private GameStateMessage currentState() {
        int whiteScore = session.gameEngine.score(Piece.Color.WHITE);
        int blackScore = session.gameEngine.score(Piece.Color.BLACK);
        boolean counting = absentUsername != null && !session.gameEngine.isGameOver();
        Integer remaining = counting ? Math.max(0, (countdownMs + 999) / 1000) : null;
        return GameStateMessage.from(
                session.gameEngine.snapshot(null),
                seats.whiteUsername().orElse(null),
                seats.blackUsername().orElse(null),
                whiteScore, blackScore, absentUsername, remaining, forfeitWinner);
    }

    private void broadcastState() {
        GameStateMessage state = currentState();
        if (state.equals(lastBroadcast)) {
            return;
        }
        lastBroadcast = state;
        messagingTemplate.convertAndSend(Topics.gameState(roomId), state);
    }

    private void broadcastStateToAll() {
        lastBroadcast = null;
        broadcastState();
    }

    private void broadcastMoveHistoryEntry(MoveMadeEvent event) {
        messagingTemplate.convertAndSend(Topics.moveHistory(roomId), toEntry(event.record()));
    }

    private MoveHistoryEntryMessage toEntry(MoveRecord record) {
        return new MoveHistoryEntryMessage(
                record.color().name(),
                notation.formatMove(record),
                MoveNotation.formatTime(record.timestamp()),
                record.timestamp());
    }

    /**
     * A move can never arrive before both seats are filled (rejectionFor blocks
     * it), so gameId is guaranteed to be set here - the null check exists only
     * as a guard against a future code path that publishes MoveMadeEvent
     * without going through that gate.
     */
    private void persistMoveHistoryEntry(MoveMadeEvent event) {
        if (gameId == null) {
            return;
        }
        MoveHistoryEntryMessage entry = toEntry(event.record());
        moveHistoryRepository.append(gameId, roomId, ++moveSeq,
                entry.color(), entry.notation(), entry.time(), entry.timestampMs());
    }

    public List<MoveHistoryEntryMessage> moveHistory() {
        return submit(() -> session.gameEngine.moveHistory().stream()
                .map(this::toEntry)
                .toList());
    }

    private void onGameOver(GameOverEvent event) {
        broadcastState();
        Piece.Color winner = decideWinner();
        ratingService.applyGameResult(
                seats.whiteUsername().orElse(null),
                seats.blackUsername().orElse(null),
                winner);
        if (gameId != null) {
            gameRepository.completeGame(gameId, winner == null ? null : winner.name());
        }
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
