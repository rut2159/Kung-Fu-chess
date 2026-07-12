package com.chessgame;

import com.chessgame.io.BoardPrinter;

import java.util.Optional;

/**
 * CommandDispatcher / מפיץ-פקודות
 *
 * תפקיד: מפרש שורת-פקודה בודדת (click/jump/wait/print board), ומנתב
 * אותה לקריאה הנכונה. לא יודע כלום על קריאת-קלט או מכונת-המצבים
 * Board-מול-Commands - מקבל GameSession מוכן ועובד רק דרכו.
 */
final class CommandDispatcher {
    private final GameSession session;
    private final BoardPrinter boardPrinter = new BoardPrinter();

    CommandDispatcher(GameSession session) {
        this.session = session;
    }

    void dispatch(String line) {
        if (line.startsWith("click")) {
            parseXY(line).ifPresent(xy -> session.controller.click(xy[0], xy[1]));
        } else if (line.startsWith("jump")) {
            parseXY(line).ifPresent(xy -> session.controller.jump(xy[0], xy[1]));
        } else if (line.startsWith("wait")) {
            parseWait(line).ifPresent(ms -> session.gameEngine.wait(ms));
        } else if (line.equals("print board")) {
            System.out.println(boardPrinter.print(session.board));
        }
    }

    private Optional<Integer> parseWait(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(parts[1]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<int[]> parseXY(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) return Optional.empty();
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            return Optional.of(new int[]{x, y});
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
