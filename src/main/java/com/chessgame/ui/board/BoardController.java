package com.chessgame.ui.board;

import com.chessgame.GameSession;
import com.chessgame.engine.GameEngine;
import com.chessgame.engine.GameListener;
import com.chessgame.engine.GameSnapshot;

import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * מחבר בין קלט-עכבר גולמי לבין רינדור הלוח. גיאומטריה (מיקום/גודל הלוח
 * בפיקסלים) ורינדור נשארים כאן; לוגיקת-הגרירה עצמה עברה ל-BoardDragHandler.
 */
public final class BoardController implements GameListener {

    private static final double MARGIN_PERCENT = 0.06;

    private record BoardGeometry(int boardSize, int offsetX, int offsetY, int cellSize) {
    }

    private final GameSession session;
    private final UiMapper uiMapper = new UiMapper();
    private final RenderUI renderUI;
    private final ChessBoardPanel boardPanel = new ChessBoardPanel();
    private final BoardDragHandler dragHandler;

    private BoardGeometry geometry;

    public BoardController(GameSession session, String whitePlayerName, String blackPlayerName) {
        this.session = session;
        this.renderUI = new RenderUI(uiMapper, whitePlayerName, blackPlayerName);
        this.dragHandler = new BoardDragHandler(session.controller);

        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point rel = relativeToBoard(e.getX(), e.getY());
                if (rel == null) {
                    return;
                }
                if (e.getClickCount() >= 2) {
                    dragHandler.doubleClick(rel, geometry.cellSize());
                } else {
                    dragHandler.beginDrag(rel, geometry.cellSize());
                }
                render();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!dragHandler.isDragging()) {
                    return;
                }
                dragHandler.endDrag(relativeToBoard(e.getX(), e.getY()));
                render();
            }
        });

        boardPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragHandler.isDragging()) {
                    return;
                }
                dragHandler.updateDrag(relativeToBoard(e.getX(), e.getY()));
                render();
            }
        });

        boardPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                recomputeGeometry();
                render();
            }
        });

        session.gameEngine.addListener(this);
        recomputeGeometry();
        render();
    }

    public ChessBoardPanel panel() {
        return boardPanel;
    }

    @Override
    public void onGameStateChanged(GameEngine engine) {
        render();
    }

    /** ממירה קואורדינטת-מסך גולמית לקואורדינטה יחסית ללוח, או null אם היא מחוץ ללוח. */
    private Point relativeToBoard(int rawX, int rawY) {
        int relX = rawX - geometry.offsetX();
        int relY = rawY - geometry.offsetY();

        boolean outsideBoard = relX < 0 || relY < 0
                || relX >= geometry.boardSize() || relY >= geometry.boardSize();
        if (outsideBoard) {
            return null;
        }
        return new Point(relX, relY);
    }

    private void render() {
        uiMapper.setCellSize(geometry.cellSize());

        GameSnapshot snapshot = session.gameEngine.snapshot(session.controller.selectedCell());
        if (dragHandler.isDragging() && dragHandler.dragPixel() != null) {
            snapshot = withDraggedPieceFollowingCursor(snapshot);
        }

        try {
            BufferedImage frameImage = renderUI.renderNewFrame(snapshot);
            boardPanel.setBoardImage(frameImage, geometry.offsetX(), geometry.offsetY());
        } catch (RuntimeException renderFailure) {
            // רשת-ביטחון: עדיף לדלג על פריים אחד (הלוח פשוט לא מתעדכן הפעם)
            // מאשר שחריגה בלתי-צפויה תקפיא את כל המשחק לצמיתות.
            System.err.println("Render skipped due to: " + renderFailure);
        }
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
    private GameSnapshot withDraggedPieceFollowingCursor(GameSnapshot snapshot) {
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

    private void recomputeGeometry() {
        int panelWidth = boardPanel.getWidth();
        int panelHeight = boardPanel.getHeight();
        int cols = session.board.width();

        int availableSize = Math.min(panelWidth, panelHeight);
        int margin = (int) (availableSize * MARGIN_PERCENT);
        int rawBoardSize = availableSize - (2 * margin);

        int cellSize = Math.max(1, rawBoardSize / cols);
        int boardSize = cellSize * cols;

        int offsetX = (panelWidth - boardSize) / 2;
        int offsetY = (panelHeight - boardSize) / 2;

        geometry = new BoardGeometry(boardSize, offsetX, offsetY, cellSize);
    }
}
