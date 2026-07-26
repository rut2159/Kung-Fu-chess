package com.chessgame.ui;

import com.chessgame.DesktopGameSession;
import com.chessgame.io.StandardBoard;
import com.chessgame.model.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameWindowTest {

    @Test
    void init_doesNotThrow_onAMachineWithARealDisplay() {
        Board board = StandardBoard.create();
        DesktopGameSession session = new DesktopGameSession(board);
        GameWindow window = new GameWindow(session, "Alice", "Bob");

        assertDoesNotThrow(window::init);
    }
}
