package com.chessgame.ui.moves;

import com.chessgame.DesktopGameSession;
import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerPanelControllerTest {

    private DesktopGameSession sessionFor(Board board) {
        return new DesktopGameSession(board);
    }

    @Test
    void panel_isNeverNull() {
        Board board = new BoardParser().parse("wK . .\n. . .\n. . .");
        DesktopGameSession session = sessionFor(board);
        PlayerPanelController controller = new PlayerPanelController(session, Piece.Color.WHITE, "Alice");

        assertNotNull(controller.panel());
    }

    @Test
    void registersAsListener_andReactsToAcceptedMove_withoutThrowing() {
        Board board = new BoardParser().parse("wR . .\n. . .\n. . .");
        DesktopGameSession session = sessionFor(board);
        new PlayerPanelController(session, Piece.Color.WHITE, "Alice");

        assertDoesNotThrow(() -> {
            session.gameEngine.requestMove(new Position(0, 0), new Position(0, 1));
            session.gameEngine.wait(1100);
        });
    }

    @Test
    void whiteAndBlackControllers_bothConstructSuccessfully() {
        Board board = new BoardParser().parse(
                "bR . .\n. . .\nwR . ."
        );
        DesktopGameSession session = sessionFor(board);

        PlayerPanelController whiteController = new PlayerPanelController(session, Piece.Color.WHITE, "Alice");
        PlayerPanelController blackController = new PlayerPanelController(session, Piece.Color.BLACK, "Bob");

        assertNotNull(whiteController.panel());
        assertNotNull(blackController.panel());
        assertNotSame(whiteController.panel(), blackController.panel());
    }
}
