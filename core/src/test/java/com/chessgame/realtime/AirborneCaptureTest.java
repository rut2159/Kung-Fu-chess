package com.chessgame.realtime;

import com.chessgame.GameSession;
import com.chessgame.bus.events.GameOverEvent;
import com.chessgame.bus.events.MoveMadeEvent;
import com.chessgame.engine.moves.MoveRecord;
import com.chessgame.io.BoardParser;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
class AirborneCaptureTest {
    private static GameSession attackerRunsIntoAJumper(String board) {
        return new GameSession(new BoardParser().parse(board));
    }

    private static List<MoveRecord> recordEvents(GameSession game) {
        List<MoveRecord> published = new ArrayList<>();
        game.gameEngine.eventBus().subscribe(MoveMadeEvent.class, event -> published.add(event.record()));
        return published;
    }

    @Test
    void jumperCapturingAnAttacker_publishesAMoveEvent() {
        GameSession game = attackerRunsIntoAJumper("""
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                bN .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                wR .  .  .  .  .  .  .
                """);
        List<MoveRecord> published = recordEvents(game);

        assertTrue(game.gameEngine.requestMove(new Position(7, 0), new Position(4, 0)).isAccepted());
        game.gameEngine.wait(2000);
        assertTrue(game.gameEngine.requestJump(new Position(4, 0)).isAccepted());
        for (int i = 0; i < 8; i++) {
            game.gameEngine.wait(500);
        }

        assertEquals(1, published.size(), "the interception must surface as a move event");

        MoveRecord record = published.get(0);
        assertEquals(Piece.Color.BLACK, record.color(), "the record belongs to the defender, not the piece that died");
        assertEquals(Piece.Kind.KNIGHT, record.kind());
        assertTrue(record.isCapture());
        assertEquals(record.source(), record.destination(), "a jump happens in place");
        assertEquals(new Position(4, 0), record.destination());

        assertEquals(1, game.gameEngine.moveHistory().size(), "and it must reach the move history");
    }

    @Test
    void jumperCapturingAKing_endsTheGame() {
        GameSession game = attackerRunsIntoAJumper("""
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                bN .  .  .  .  .  .  .
                wK .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                """);
        boolean[] gameOver = {false};
        game.gameEngine.eventBus().subscribe(GameOverEvent.class, event -> gameOver[0] = true);

        assertTrue(game.gameEngine.requestMove(new Position(5, 0), new Position(4, 0)).isAccepted());
        game.gameEngine.wait(300);
        assertTrue(game.gameEngine.requestJump(new Position(4, 0)).isAccepted());
        for (int i = 0; i < 6; i++) {
            game.gameEngine.wait(300);
        }

        assertTrue(gameOver[0], "capturing a king is a win however it happened");
    }

    @Test
    void anOrdinaryCapture_isNotMarkedAsAJump() {
        GameSession game = attackerRunsIntoAJumper("""
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                bN .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                wR .  .  .  .  .  .  .
                """);
        List<MoveRecord> published = recordEvents(game);

        assertTrue(game.gameEngine.requestMove(new Position(7, 0), new Position(4, 0)).isAccepted());
        for (int i = 0; i < 8; i++) {
            game.gameEngine.wait(500);
        }

        assertEquals(1, published.size());
        MoveRecord record = published.get(0);
        assertEquals(Piece.Color.WHITE, record.color());
        assertTrue(record.isCapture());
        assertNotEquals(record.source(), record.destination(),
                "an ordinary capture moves, so it can never be mistaken for an interception");
    }
}