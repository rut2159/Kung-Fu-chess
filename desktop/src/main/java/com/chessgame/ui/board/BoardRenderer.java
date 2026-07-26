package com.chessgame.ui.board;

import com.chessgame.engine.snapshot.GameSnapshot;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

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
