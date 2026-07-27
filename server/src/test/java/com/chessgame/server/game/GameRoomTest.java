package com.chessgame.server.game;

import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.server.dto.GameStateMessage;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.repository.UserRepository;
import com.chessgame.server.service.RatingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameRoomTest {

    private static final String ROOM_ID = "K7F2QX";

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private GameRoom newRoom(String boardText) {
        Board board = new BoardParser().parse(boardText);
        return new GameRoom(ROOM_ID, board, new RatingService(userRepository), messagingTemplate);
    }

    private GameRoom newRoomWithTwoPlayers(String boardText) {
        GameRoom room = newRoom(boardText);
        room.join("alice");
        room.join("bob");
        return room;
    }

    @Test
    void whitePlayerCanMoveAWhitePiece() {
        GameRoom room = newRoomWithTwoPlayers("wR . .");
        assertTrue(room.handleMove(new MoveCommand(0, 0, 0, 1), "alice").isAccepted());
    }

    @Test
    void blackPlayerCanMoveABlackPiece() {
        GameRoom room = newRoomWithTwoPlayers("wK . .\nbR . .");
        assertTrue(room.handleMove(new MoveCommand(1, 0, 1, 1), "bob").isAccepted());
    }

    @Test
    void blackPlayerCannotMoveAWhitePiece() {
        GameRoom room = newRoomWithTwoPlayers("wR . .");
        assertFalse(room.handleMove(new MoveCommand(0, 0, 0, 1), "bob").isAccepted());
    }

    @Test
    void viewerCannotMoveAnyPiece() {
        GameRoom room = newRoomWithTwoPlayers("wR . .");
        room.join("carol");
        assertFalse(room.handleMove(new MoveCommand(0, 0, 0, 1), "carol").isAccepted());
    }

    @Test
    void moveIsRejectedWhileOnlyOnePlayerHasJoined() {
        GameRoom room = newRoom("wR . .");
        room.join("alice");
        assertFalse(room.handleMove(new MoveCommand(0, 0, 0, 1), "alice").isAccepted());
    }

    @Test
    void acceptedMoveIsBroadcastOnTheRoomTopic() {
        GameRoom room = newRoomWithTwoPlayers("wR . .");

        room.handleMove(new MoveCommand(0, 0, 0, 1), "alice");

        verify(messagingTemplate, atLeastOnce())
                .convertAndSend(eq(Topics.gameState(ROOM_ID)), any(GameStateMessage.class));
    }

    @Test
    void twoRoomsBroadcastOnSeparateTopics() {
        GameRoom first = newRoomWithTwoPlayers("wR . .");
        Board board = new BoardParser().parse("wR . .");
        GameRoom second = new GameRoom("ABCDEF", board, new RatingService(userRepository), messagingTemplate);
        second.join("carol");
        second.join("dave");

        first.handleMove(new MoveCommand(0, 0, 0, 1), "alice");
        second.handleMove(new MoveCommand(0, 0, 0, 1), "carol");

        verify(messagingTemplate, atLeastOnce())
                .convertAndSend(eq(Topics.gameState(ROOM_ID)), any(GameStateMessage.class));
        verify(messagingTemplate, atLeastOnce())
                .convertAndSend(eq(Topics.gameState("ABCDEF")), any(GameStateMessage.class));
    }

    @Test
    void seatsAreReportedInTheState() {
        GameRoom room = newRoomWithTwoPlayers("wR . .");
        assertEquals("alice", room.whiteUsername().orElseThrow());
        assertEquals("bob", room.blackUsername().orElseThrow());
    }

    @Test
    void concurrentMoveAndTimeAdvanceDoNotCorruptState() throws InterruptedException {
        GameRoom room = newRoomWithTwoPlayers("wR . .");
        CountDownLatch done = new CountDownLatch(2);

        Runnable mover = () -> {
            for (int i = 0; i < 50; i++) {
                room.handleMove(new MoveCommand(0, 0, 0, 1), "alice");
            }
            done.countDown();
        };
        Runnable ticker = () -> {
            for (int i = 0; i < 50; i++) {
                room.advanceTimeAndBroadcast(10);
            }
            done.countDown();
        };

        new Thread(mover).start();
        new Thread(ticker).start();

        assertTrue(done.await(10, TimeUnit.SECONDS));
    }
}
