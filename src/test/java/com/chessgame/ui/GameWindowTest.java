package com.chessgame.ui;

import com.chessgame.GameSession;
import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameWindowTest {

    @Test
    void init_doesNotThrow_onAMachineWithARealDisplay() {
        Board board = new BoardParser().parse(
                "bR bN bB bQ bK bB bN bR\n" +
                "bP bP bP bP bP bP bP bP\n" +
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  .  .  .  .\n" +
                "wP wP wP wP wP wP wP wP\n" +
                "wR wN wB wQ wK wB wN wR");
        GameSession session = new GameSession(board);
        GameWindow window = new GameWindow(session, "Alice", "Bob");

        assertDoesNotThrow(window::init);
    }
}
