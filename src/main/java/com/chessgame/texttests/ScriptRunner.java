package com.chessgame.texttests;

import com.chessgame.engine.GameEngine;
import com.chessgame.input.BoardMapper;
import com.chessgame.input.Controller;
import com.chessgame.io.BoardParser;
import com.chessgame.io.BoardPrinter;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.rules.PieceRules;
import com.chessgame.rules.RuleEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ScriptRunner / מריץ-סקריפט
 *
 * תפקיד: מבצע Script בפועל - "עושה בדיוק מה שמשתמש עושה", דרך
 * הנתיב הציבורי האמיתי בלבד. בדיוק לפי הזרימה מהמסמך:
 *
 *   click       -> Controller.click(x, y)
 *   wait        -> GameEngine.wait(ms)
 *   print board -> BoardPrinter.print(board), משווה לציפייה
 *
 * אסור-במפורש (המסמך): קריאה ישירה ל-Board.movePiece - זה היה
 * "עוקף" את Controller/GameEngine/RuleEngine/RealTimeArbiter, והטסט
 * כבר לא היה מוכיח שהנתיב האמיתי-של-המשתמש עובד. שימי לב: אין כאן
 * שום קריאה כזו בכלל - אין אפילו import ל-Board.movePiece.
 */
public final class ScriptRunner {

    /** Mismatch / אי-התאמה - תוצאה של print-board אחד שלא תאם לציפייה. */
    public static final class Mismatch {
        public final int printIndex;
        public final List<String> expected;
        public final List<String> actual;

        public Mismatch(int printIndex, List<String> expected, List<String> actual) {
            this.printIndex = printIndex;
            this.expected = expected;
            this.actual = actual;
        }
    }

    /** מריץ סקריפט מקצה-לקצה. מחזיר רשימת אי-התאמות - ריקה = הסקריפט עבר במלואו. */
    public List<Mismatch> run(Script script) {
        Board board = new BoardParser().parse(script.boardText());
        GameState gameState = new GameState();
        RuleEngine ruleEngine = new RuleEngine(board, new PieceRules());
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        GameEngine gameEngine = new GameEngine(board, gameState, ruleEngine, arbiter);
        Controller controller = new Controller(board, new BoardMapper(board), gameEngine);
        BoardPrinter boardPrinter = new BoardPrinter();

        List<Mismatch> mismatches = new ArrayList<>();
        int printIndex = 0;

        for (Script.Command command : script.commands()) {
            if (command instanceof Script.ClickCommand) {
                Script.ClickCommand click = (Script.ClickCommand) command;
                controller.click(click.x, click.y);

            } else if (command instanceof Script.JumpCommand) {
                Script.JumpCommand jump = (Script.JumpCommand) command;
                controller.jump(jump.x, jump.y);

            } else if (command instanceof Script.WaitCommand) {
                Script.WaitCommand wait = (Script.WaitCommand) command;
                gameEngine.wait(wait.milliseconds);

            } else if (command instanceof Script.PrintBoardCommand) {
                Script.PrintBoardCommand print = (Script.PrintBoardCommand) command;
                List<String> actualRows = Arrays.asList(boardPrinter.print(board).split("\n"));
                if (!actualRows.equals(print.expectedRows)) {
                    mismatches.add(new Mismatch(printIndex, print.expectedRows, actualRows));
                }
                printIndex++;
            }
        }

        return mismatches;
    }
}
