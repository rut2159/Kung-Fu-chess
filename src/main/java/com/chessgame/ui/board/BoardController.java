package com.chessgame.ui.board;

import com.chessgame.GameSession;
import com.chessgame.engine.GameEngine;
import com.chessgame.engine.listeners.GameListener;

import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

/**
 * "חיווט" בלבד: מחבר בין אירועי-עכבר גולמיים, גיאומטריית-הלוח, ורינדור.
 * לא מיישם אף אחד מהם בעצמו - BoardDragHandler מטפל בקלט, BoardGeometry
 * מחשבת מיקום/גודל, BoardRenderer בונה את התמונה.
 */
public final class BoardController implements GameListener {

    private final GameSession session;
    private final ChessBoardPanel boardPanel = new ChessBoardPanel();
    private final BoardDragHandler dragHandler;
    private final BoardRenderer renderer;

    private BoardGeometry geometry;

    public BoardController(GameSession session, String whitePlayerName, String blackPlayerName) {
        this.session = session;
        this.dragHandler = new BoardDragHandler(session.controller);

        UiMapper uiMapper = new UiMapper();
        RenderUI renderUI = new RenderUI(uiMapper, whitePlayerName, blackPlayerName);
        this.renderer = new BoardRenderer(uiMapper, renderUI);

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

        if (!geometry.containsRelative(relX, relY)) {
            return null;
        }
        return new Point(relX, relY);
    }

    private void render() {
        try {
            BufferedImage frameImage = renderer.render(
                    session.gameEngine.snapshot(session.controller.selectedCell()), geometry, dragHandler);
            boardPanel.setBoardImage(frameImage, geometry.offsetX(), geometry.offsetY());
        } catch (RuntimeException renderFailure) {
            // רשת-ביטחון: עדיף לדלג על פריים אחד מאשר שחריגה בלתי-צפויה
            // תקפיא את כל המשחק לצמיתות.
            System.err.println("Render skipped due to: " + renderFailure);
        }
    }

    private void recomputeGeometry() {
        geometry = BoardGeometry.compute(boardPanel.getWidth(), boardPanel.getHeight(), session.board.width());
    }
}
