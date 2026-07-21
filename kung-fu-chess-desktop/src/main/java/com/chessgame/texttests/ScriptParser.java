package com.chessgame.texttests;

import java.util.ArrayList;
import java.util.List;

public final class ScriptParser {

    public static final class ScriptParseException extends RuntimeException {
        public ScriptParseException(String message) {
            super(message);
        }
    }

    public Script parse(String scriptText) {
        List<String> lines = new ArrayList<>();
        for (String line : scriptText.split("\n", -1)) {
            lines.add(line);
        }

        int i = skipBlankLines(lines, 0);

        if (i >= lines.size() || !lines.get(i).trim().equals("Board")) {
            throw new ScriptParseException("Script must start with 'Board'");
        }
        i++;

        List<String> boardRows = new ArrayList<>();
        while (i < lines.size() && !lines.get(i).trim().isEmpty()) {
            boardRows.add(lines.get(i).trim());
            i++;
        }
        int boardHeight = boardRows.size();
        String boardText = String.join("\n", boardRows);

        i = skipBlankLines(lines, i);

        List<Script.Command> commands = new ArrayList<>();
        while (i < lines.size()) {
            String line = lines.get(i).trim();

            if (line.isEmpty()) {
                i++;
                continue;
            }

            if (line.startsWith("click")) {
                String[] parts = line.split("\\s+");
                commands.add(new Script.ClickCommand(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                i++;
            } else if (line.startsWith("jump")) {
                String[] parts = line.split("\\s+");
                commands.add(new Script.JumpCommand(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                i++;
            } else if (line.startsWith("wait")) {
                String[] parts = line.split("\\s+");
                commands.add(new Script.WaitCommand(Integer.parseInt(parts[1])));
                i++;
            } else if (line.equals("print board")) {
                i++;
                List<String> expectedRows = new ArrayList<>();
                for (int r = 0; r < boardHeight && i < lines.size(); r++, i++) {
                    expectedRows.add(lines.get(i).trim());
                }
                commands.add(new Script.PrintBoardCommand(expectedRows));
            } else {
                throw new ScriptParseException("Unknown command: '" + line + "'");
            }
        }

        return new Script(boardText, commands);
    }

    private int skipBlankLines(List<String> lines, int from) {
        int i = from;
        while (i < lines.size() && lines.get(i).trim().isEmpty()) i++;
        return i;
    }
}
