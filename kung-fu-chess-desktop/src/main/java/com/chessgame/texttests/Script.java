package com.chessgame.texttests;

import java.util.Collections;
import java.util.List;

public final class Script {
    private final String boardText;
    private final List<Command> commands;

    public Script(String boardText, List<Command> commands) {
        this.boardText = boardText;
        this.commands = Collections.unmodifiableList(commands);
    }

    public String boardText() {
        return boardText;
    }

    public List<Command> commands() {
        return commands;
    }

    public interface Command {
    }

    public static final class ClickCommand implements Command {
        public final int x;
        public final int y;

        public ClickCommand(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static final class JumpCommand implements Command {
        public final int x;
        public final int y;

        public JumpCommand(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static final class WaitCommand implements Command {
        public final int milliseconds;

        public WaitCommand(int milliseconds) {
            this.milliseconds = milliseconds;
        }
    }

    public static final class PrintBoardCommand implements Command {
        public final List<String> expectedRows;

        public PrintBoardCommand(List<String> expectedRows) {
            this.expectedRows = Collections.unmodifiableList(expectedRows);
        }
    }
}
