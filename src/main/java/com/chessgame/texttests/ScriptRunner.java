package com.chessgame.texttests;

import com.chessgame.GameSession;
import com.chessgame.io.BoardParser;
import com.chessgame.io.BoardPrinter;
import com.chessgame.model.Board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ScriptRunner {

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

    public List<Mismatch> run(Script script) {
        Board board = new BoardParser().parse(script.boardText());
        GameSession session = new GameSession(board);
        BoardPrinter boardPrinter = new BoardPrinter();

        List<Mismatch> mismatches = new ArrayList<>();
        int printIndex = 0;

        for (Script.Command command : script.commands()) {
            if (command instanceof Script.ClickCommand) {
                Script.ClickCommand click = (Script.ClickCommand) command;
                session.controller.click(click.x, click.y);

            } else if (command instanceof Script.JumpCommand) {
                Script.JumpCommand jump = (Script.JumpCommand) command;
                session.controller.jump(jump.x, jump.y);

            } else if (command instanceof Script.WaitCommand) {
                Script.WaitCommand wait = (Script.WaitCommand) command;
                session.gameEngine.wait(wait.milliseconds);

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
