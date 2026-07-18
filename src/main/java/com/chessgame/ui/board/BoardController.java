package com.chessgame.ui.board;

import com.chessgame.GameSession;
import com.chessgame.engine.GameEngine;
import com.chessgame.engine.GameListener;
import com.chessgame.engine.GameSnapshot;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public final class BoardController implements GameListener {

    private static final double MARGIN_PERCENT = 0.06;

    private record BoardGeometry(int boardSize, int offsetX, int offsetY, int cellSize) {
    }

    private final GameSession session;
    private final UiMapper uiMapper = new UiMapper();
    private final RenderUI renderUI;
    private final ChessBoardPanel boardPanel = new ChessBoardPanel();

    private BoardGeometry geometry;

    public BoardController(GameSession session, String whitePlayerName, String blackPlayerName) {
        this.session = session;
        this.renderUI = new RenderUI(uiMapper, whitePlayerName, blackPlayerName);

        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
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

    private void handleClick(int rawX, int rawY) {
        int relX = rawX - geometry.offsetX();
        int relY = rawY - geometry.offsetY();

        boolean outsideBoard = relX < 0 || relY < 0
                || relX >= geometry.boardSize() || relY >= geometry.boardSize();
        if (outsideBoard) {
            return;
        }

        session.controller.setCellSizePx(geometry.cellSize());
        session.controller.click(relX, relY);
    }

    private void render() {
        uiMapper.setCellSize(geometry.cellSize());

        GameSnapshot snapshot = session.gameEngine.snapshot(session.controller.selectedCell());
        BufferedImage frameImage = renderUI.renderNewFrame(snapshot);

        boardPanel.setBoardImage(frameImage, geometry.offsetX(), geometry.offsetY());
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
