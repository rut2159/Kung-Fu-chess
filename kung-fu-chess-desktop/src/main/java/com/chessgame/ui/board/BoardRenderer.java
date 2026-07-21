package com.chessgame.ui.board;

import com.chessgame.engine.snapshot.GameSnapshot;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * אחראית אך ורק על בניית תמונת-הפריים לרינדור, כולל "הצמדת" הכלי-הנגרר
 * לעכבר. לא יודעת כלום על קלט-עכבר גולמי או Swing panels/listeners.
 */
final class BoardRenderer {
    private final UiMapper uiMapper;
    private final RenderUI renderUI;

    BoardRenderer(UiMapper uiMapper, RenderUI renderUI) {
        this.uiMapper = uiMapper;
        this.renderUI = renderUI;
    }

    BufferedImage render(GameSnapshot snapshot, BoardGeometry geometry, BoardDragHandler dragHandler) {
        uiMapper.setCellSize(geometry.cellSize());

        GameSnapshot toRender = snapshot;
        if (dragHandler.isDragging() && dragHandler.dragPixel() != null) {
            toRender = withDraggedPieceFollowingCursor(toRender, geometry, dragHandler);
        }

        return renderUI.renderNewFrame(toRender);
    }

    /**
     * מחליפה את מיקום-התצוגה של הכלי הנגרר בלבד, כך שהוא "נצמד" לעכבר - שאר
     * הכלים לא מושפעים. חשוב: הציור (drawPiece) ממקם את הפינה השמאלית-
     * עליונה של הספרייט בקואורדינטה שמחזירים כאן - לכן מזיזים אחורה חצי
     * משבצת בכל ציר, כדי שהמרכז של הכלי (לא הפינה שלו) ייצמד בפועל לעכבר.
     *
     * חשוב עוד יותר: קרוב לקצה הימני/תחתון של הלוח, ה"מרכוז" הזה יכול
     * לדחוף את הספרייט מעבר לגבול תמונת-הלוח - לכן חובה לצבוט (clamp)
     * את המיקום כך שהוא לעולם לא יגרום לחריגה.
     */
    private GameSnapshot withDraggedPieceFollowingCursor(GameSnapshot snapshot, BoardGeometry geometry, BoardDragHandler dragHandler) {
        Point dragPixel = dragHandler.dragPixel();
        double fractionalRow = ((double) dragPixel.y - geometry.cellSize() / 2.0) / geometry.cellSize();
        double fractionalCol = ((double) dragPixel.x - geometry.cellSize() / 2.0) / geometry.cellSize();

        fractionalRow = clamp(fractionalRow, 0, snapshot.height() - 1);
        fractionalCol = clamp(fractionalCol, 0, snapshot.width() - 1);

        List<GameSnapshot.PieceView> updated = new ArrayList<>();
        for (GameSnapshot.PieceView piece : snapshot.pieces()) {
            if (piece.position().equals(dragHandler.dragSource())) {
                updated.add(piece.withDisplayPosition(fractionalRow, fractionalCol));
            } else {
                updated.add(piece);
            }
        }

        return new GameSnapshot(snapshot.width(), snapshot.height(), updated,
                snapshot.selectedCell(), snapshot.isGameOver(), snapshot.winner());
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
