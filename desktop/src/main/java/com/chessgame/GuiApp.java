package com.chessgame;

import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.ui.GameWindow;
import com.chessgame.ui.WelcomeScreen;

import javax.swing.SwingUtilities;

public final class GuiApp {

    private static final String STARTING_POSITION =
            "bR bN bB bQ bK bB bN bR\n" +
            "bP bP bP bP bP bP bP bP\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            "wP wP wP wP wP wP wP wP\n" +
            "wR wN wB wQ wK wB wN wR";

    public static void main(String[] args) {
        Board board = new BoardParser().parse(STARTING_POSITION);
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
