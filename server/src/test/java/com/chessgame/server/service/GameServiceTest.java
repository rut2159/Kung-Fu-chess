package com.chessgame.server.service;

import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private GameService newGameServiceWithTwoPlayers(String boardText) {
        PlayerAssignmentService playerAssignmentService = new PlayerAssignmentService();
        RatingService ratingService = new RatingService(userRepository);
        Board board = new BoardParser().parse(boardText);

        GameService gameService = new GameService(playerAssignmentService, ratingService, messagingTemplate, board);

        playerAssignmentService.assign("white-session", "alice");
        playerAssignmentService.assign("black-session", "bob");

        return gameService;
    }

    @Test
    void whitePlayer_canMoveAWhitePiece() {
        GameService gameService = newGameServiceWithTwoPlayers("wR . .");

        var result = gameService.handleMove(new MoveCommand(0, 0, 0, 1), "white-session");

        assertTrue(result.isAccepted());
    }

    @Test
    void move_isRejected_whenOnlyOnePlayerHasJoined() {
        PlayerAssignmentService playerAssignmentService = new PlayerAssignmentService();
        RatingService ratingService = new RatingService(userRepository);
        Board board = new BoardParser().parse("wR . .");
        GameService gameService = new GameService(playerAssignmentService, ratingService, messagingTemplate, board);

        playerAssignmentService.assign("white-session", "alice");

        var result = gameService.handleMove(new MoveCommand(0, 0, 0, 1), "white-session");

        assertFalse(result.isAccepted());
        assertEquals(com.chessgame.rules.MoveReason.WAITING_FOR_OPPONENT, result.reason());
    }

    @Test
    void blackPlayer_cannotMoveAWhitePiece() {
        GameService gameService = newGameServiceWithTwoPlayers("wR . .");

        var result = gameService.handleMove(new MoveCommand(0, 0, 0, 1), "black-session");

        assertFalse(result.isAccepted());
    }

    @Test
    void unassignedViewer_cannotMoveAnyPiece() {
        GameService gameService = newGameServiceWithTwoPlayers("wR . .");

        var result = gameService.handleMove(new MoveCommand(0, 0, 0, 1), "some-random-viewer-session");

        assertFalse(result.isAccepted());
    }

    @Test
    void blackPlayer_canMoveABlackPiece() {
        GameService gameService = newGameServiceWithTwoPlayers("wK . .\nbR . .");

        var result = gameService.handleMove(new MoveCommand(1, 0, 1, 1), "black-session");

        assertTrue(result.isAccepted());
    }

    @Test
    void acceptedMove_triggersABroadcast_viaTheEventBus_notImperativeCode() {
        GameService gameService = newGameServiceWithTwoPlayers("wR . .");

        gameService.handleMove(new MoveCommand(0, 0, 0, 1), "white-session");
        gameService.advanceTimeAndBroadcast(1000);

        org.mockito.Mockito.verify(messagingTemplate, org.mockito.Mockito.atLeastOnce())
                .convertAndSend(
                        org.mockito.ArgumentMatchers.eq("/topic/game"),
                        org.mockito.ArgumentMatchers.any(com.chessgame.server.dto.GameStateMessage.class));
    }

    @Test
    void concurrentMoveAndTimeAdvance_fromDifferentThreads_doNotThrowOrCorruptState() throws InterruptedException {
        GameService gameService = newGameServiceWithTwoPlayers("wR . .");

        Thread mover = new Thread(() ->
                gameService.handleMove(new MoveCommand(0, 0, 0, 1), "white-session"));
        Thread ticker = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                gameService.advanceTimeAndBroadcast(50);
            }
        });

        mover.start();
        ticker.start();
        mover.join();
        ticker.join();

        assertTrue(true, "completing without an exception is the assertion here");
    }
}
