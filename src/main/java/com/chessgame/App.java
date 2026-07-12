package com.chessgame;

import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * App / אפליקציה
 *
 * תפקיד: קורא מ-System.in שורה-שורה, ומנתב: שורות-לוח נאספות
 * (עד "Commands:"), ושורות-פקודה מועברות ל-CommandDispatcher.
 * לא בונה בעצמה את שכבות-המשחק (זה GameSession), ולא מפרשת בעצמה
 * תוכן-פקודה (זה CommandDispatcher) - רק לולאת-קריאה + מכונת-מצבים
 * דקה (Board-section מול Commands-section).
 */
public final class App {

    private enum ParsingState { INIT, PARSING_BOARD, PARSING_COMMANDS }

    private final List<String> boardLines = new ArrayList<>();
    private ParsingState state = ParsingState.INIT;
    private CommandDispatcher dispatcher;

    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line.trim());
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
    }

    private void processLine(String line) {
        if (line.isEmpty()) return;

        if (line.equals("Board:")) {
            state = ParsingState.PARSING_BOARD;
            return;
        }
        if (line.startsWith("Commands:")) {
            dispatcher = buildDispatcher();
            state = ParsingState.PARSING_COMMANDS;
            return;
        }

        if (state == ParsingState.PARSING_BOARD) {
            boardLines.add(line);
        } else if (state == ParsingState.PARSING_COMMANDS) {
            dispatcher.dispatch(line);
        }
    }

    private CommandDispatcher buildDispatcher() {
        Board board;
        try {
            board = new BoardParser().parse(String.join("\n", boardLines));
        } catch (BoardParser.BoardParseException e) {
            System.out.println("ERROR " + e.getMessage());
            System.exit(0);
            return null; // System.exit כבר עוצר את התהליך - שורה זו לעולם לא רצה בפועל
        }
        return new CommandDispatcher(new GameSession(board));
    }
}
