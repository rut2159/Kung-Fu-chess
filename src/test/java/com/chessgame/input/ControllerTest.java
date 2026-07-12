package com.chessgame.input;

import com.chessgame.engine.GameEngine;
import com.chessgame.engine.MoveResult;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.io.BoardParser;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.rules.PieceRules;
import com.chessgame.rules.RuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ControllerTest {

    private Board board;
    private Controller controller;

    @BeforeEach
    void setUp() {
        board = new BoardParser().parse("wK bR .\n. . .\n. . .");
        GameState gameState = new GameState();
        RuleEngine ruleEngine = new RuleEngine(board, new PieceRules());
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        GameEngine engine = new GameEngine(board, gameState, ruleEngine, arbiter);
        BoardMapper mapper = new BoardMapper(board);
        controller = new Controller(board, mapper, engine);
    }

    @Test
    void firstClickOnEmptyCell_isIgnored() {
        ControllerResult result = controller.click(150, 50); // (0,1) - ריק

        assertFalse(result.requestedMove());
    }

    @Test
    void firstClickOnAPiece_selectsItWithoutRequestingAMove() {
        ControllerResult result = controller.click(50, 50); // (0,0) - wK

        assertFalse(result.requestedMove()); // בחירה בלבד, אין פקודה ל-GameEngine עדיין
    }

    @Test
    void secondClickOnEmptyCell_requestsAMove() {
        controller.click(50, 50);              // בוחר wK ב-(0,0)
        ControllerResult result = controller.click(50, 150); // (1,0) ריק, בקו-ישר, חוקי למלך

        assertTrue(result.requestedMove());
        assertTrue(result.moveResult().isAccepted());
    }

    @Test
    void secondClickOnFriendlyPiece_reselectsInsteadOfMoving() {
        controller.click(50, 50); // wK
        // אין כלי לבן שני בלוח הזה - נוסיף תרחיש עם שני כלים לבנים
        board = new BoardParser().parse("wK wR .\n. . .\n. . .");
        GameState gameState = new GameState();
        GameEngine engine = new GameEngine(board, gameState, new RuleEngine(board, new PieceRules()), new RealTimeArbiter(board));
        controller = new Controller(board, new BoardMapper(board), engine);

        controller.click(50, 50);              // בוחר wK
        ControllerResult result = controller.click(150, 50);  // wR - ידידותי, בחירה-מחדש

        assertFalse(result.requestedMove()); // לא נשלח מהלך - רק הוחלפה הבחירה
    }

    @Test
    void clickOutsideBoard_withNoSelection_isIgnored() {
        ControllerResult result = controller.click(-10, -10);

        assertFalse(result.requestedMove());
    }

    @Test
    void clickOutsideBoard_withActiveSelection_cancelsSelectionWithoutSendingACommand() {
        controller.click(50, 50); // בוחר wK

        ControllerResult result = controller.click(-10, -10); // מבטל

        assertFalse(result.requestedMove()); // בלי שום פקודה ל-GameEngine

        // הוכחה שהבחירה באמת בוטלה: קליק הבא הוא "קליק ראשון" חדש
        // (על תא-ריק) - לא "קליק שני" שהיה שולח מהלך
        ControllerResult next = controller.click(50, 150);
        assertFalse(next.requestedMove());
    }

    @Test
    void secondClickAlwaysClearsSelection_evenWhenMoveIsRejected() {
        controller.click(50, 50); // בוחר wK
        controller.click(250, 250); // יעד רחוק/לא-חוקי למלך - נדחה

        // הבחירה חייבת להתאפס בכל מקרה - קליק הבא הוא "קליק ראשון" חדש
        ControllerResult next = controller.click(50, 150);
        assertFalse(next.requestedMove()); // רק בחירה, לא מהלך - כלומר זה נחשב "ראשון"
    }

    @Test
    void jump_requestsAJumpDirectly() {
        ControllerResult result = controller.jump(150, 50); // bR ב-(0,1)

        assertTrue(result.requestedMove());
        assertTrue(result.moveResult().isAccepted());
    }
}
