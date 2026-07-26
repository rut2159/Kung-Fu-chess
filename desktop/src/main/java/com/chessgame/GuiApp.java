package com.chessgame;

import com.chessgame.io.StandardBoard;
import com.chessgame.model.Board;
import com.chessgame.ui.GameWindow;
import com.chessgame.ui.WelcomeScreen;

import javax.swing.SwingUtilities;

public final class GuiApp {

    public static void main(String[] args) {
        Board board = StandardBoard.create();
        DesktopGameSession session = new DesktopGameSession(board);

        SwingUtilities.invokeLater(() -> {
            WelcomeScreen welcome = new WelcomeScreen((whiteName, blackName) -> {
                GameWindow window = new GameWindow(session, whiteName, blackName);
                window.init();
            });
            welcome.setVisible(true);
        });
    }
}
