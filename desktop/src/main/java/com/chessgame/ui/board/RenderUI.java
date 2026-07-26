package com.chessgame.ui.board;

import com.chessgame.engine.snapshot.GameSnapshot;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class RenderUI {
    private static final String BOARD_IMAGE_PATH = "/board.png";
    private static final Color SELECTED_CELL_BORDER_COLOR = new Color(255, 255, 255, 230);
    private static final int SELECTED_CELL_BORDER_THICKNESS = 4;
    private static final Color COOLDOWN_HIGHLIGHT_COLOR = new Color(212, 175, 55, 160);
    private static final Color PREMOVE_HIGHLIGHT_COLOR = new Color(66, 133, 244, 160);
    private static final Color COORDINATE_LABEL_COLOR = new Color(255, 255, 255, 210);
    private static final Color GAME_OVER_BACKGROUND = new Color(0, 0, 0, 235);
    private static final Color GAME_OVER_TITLE_COLOR = Color.WHITE;
    private static final Color GAME_OVER_WINNER_COLOR = new Color(212, 175, 55);

    private final UiMapper mapper;
    private final String whitePlayerName;
    private final String blackPlayerName;
    private final ImageCache imageCache = new ImageCache();
    private final SpriteResolver spriteResolver = new SpriteResolver();

    public RenderUI(UiMapper mapper, String whitePlayerName, String blackPlayerName) {
        this.mapper = mapper;
        this.whitePlayerName = whitePlayerName;
        this.blackPlayerName = blackPlayerName;
    }

    public BufferedImage renderNewFrame(GameSnapshot snapshot) {
        int cellSize = mapper.getCellSize();
        int imageWidth = cellSize * snapshot.width();
        int imageHeight = cellSize * snapshot.height();

        BufferedImage cachedBackground = imageCache.get(BOARD_IMAGE_PATH, imageWidth, imageHeight, false);
        Img finalBoardImage = Img.wrap(cachedBackground).copy();

        if (snapshot.selectedCell() != null) {
            drawSelectedCellHighlight(finalBoardImage, snapshot.selectedCell(), cellSize);
        }

        drawCoordinateLabels(finalBoardImage, cellSize, snapshot.width(), snapshot.height());

        for (GameSnapshot.PieceView piece : snapshot.pieces()) {
            drawPremoveHighlight(finalBoardImage, piece, cellSize);
            drawCooldownHighlight(finalBoardImage, piece, cellSize);
            drawPiece(finalBoardImage, piece, cellSize);
        }

        if (snapshot.isGameOver()) {
            drawGameOverText(finalBoardImage, snapshot.winner());
        }

        return finalBoardImage.get();
    }

    private void drawSelectedCellHighlight(Img boardImage, Position selectedCell, int cellSize) {
        Point pixel = mapper.cellToPixel(selectedCell);
        boardImage.drawRect(pixel.x, pixel.y, cellSize, cellSize,
                SELECTED_CELL_BORDER_COLOR, SELECTED_CELL_BORDER_THICKNESS);
    }

    private void drawCooldownHighlight(Img boardImage, GameSnapshot.PieceView piece, int cellSize) {
        if (piece.cooldownRemaining() <= 0.0) {
            return;
        }
        int highlightHeight = (int) Math.round(cellSize * piece.cooldownRemaining());

        Point pixel = mapper.cellToPixel(piece.displayRow(), piece.displayCol());
        int topY = pixel.y + (cellSize - highlightHeight);
        boardImage.fillRect(pixel.x, topY, cellSize, highlightHeight, COOLDOWN_HIGHLIGHT_COLOR);
    }

    private void drawPremoveHighlight(Img boardImage, GameSnapshot.PieceView piece, int cellSize) {
        if (!piece.hasPremove()) {
            return;
        }
        Point pixel = mapper.cellToPixel(piece.displayRow(), piece.displayCol());
        boardImage.fillRect(pixel.x, pixel.y, cellSize, cellSize, PREMOVE_HIGHLIGHT_COLOR);
    }

    private void drawCoordinateLabels(Img boardImage, int cellSize, int cols, int rows) {
        int inset = Math.max(3, cellSize / 14);
        float fontSize = Math.max(0.7f, cellSize / 90f);

        for (int col = 0; col < cols; col++) {
            char file = (char) ('a' + col);
            Point pixel = mapper.cellToPixel(rows - 1, col);
            int x = pixel.x + inset;
            int y = pixel.y + cellSize - inset;
            boardImage.putText(String.valueOf(file), x, y, fontSize, COORDINATE_LABEL_COLOR, 1);
        }

        for (int row = 0; row < rows; row++) {
            int rank = rows - row;
            Point pixel = mapper.cellToPixel(row, 0);
            int x = pixel.x + inset;
            int y = pixel.y + inset + (int) (fontSize * 12);
            boardImage.putText(String.valueOf(rank), x, y, fontSize, COORDINATE_LABEL_COLOR, 1);
        }
    }

    private void drawPiece(Img boardImage, GameSnapshot.PieceView piece, int cellSize) {
        String path = spriteResolver.spritePath(piece);
        if (path == null) {
            return;
        }

        BufferedImage cachedPiece = imageCache.get(path, cellSize, cellSize, true);
        Img pieceImg = Img.wrap(cachedPiece);
        Point pixel = mapper.cellToPixel(piece.displayRow(), piece.displayCol());
        pieceImg.drawOn(boardImage, pixel.x, pixel.y);
    }

    private void drawGameOverText(Img boardImage, Piece.Color winner) {
        int imgWidth = boardImage.get().getWidth();
        int imgHeight = boardImage.get().getHeight();

        int bandHeight = imgHeight / 3;
        int bandY = (imgHeight - bandHeight) / 2;
        boardImage.fillRect(0, bandY, imgWidth, bandHeight, GAME_OVER_BACKGROUND);

        int centerX = imgWidth / 2;
        int titleY = bandY + bandHeight / 2 - bandHeight / 6;
        int winnerY = bandY + bandHeight / 2 + bandHeight / 4;

        float titleFontSize = Math.max(2.5f, imgWidth / 220f);
        float winnerFontSize = Math.max(1.6f, imgWidth / 320f);

        boardImage.putTextCentered("GAME OVER", centerX, titleY, titleFontSize, GAME_OVER_TITLE_COLOR);

        String winnerName = (winner == Piece.Color.WHITE) ? whitePlayerName
                : (winner == Piece.Color.BLACK) ? blackPlayerName : null;
        if (winnerName != null) {
            boardImage.putTextCentered(winnerName + " wins!", centerX, winnerY,
                    winnerFontSize, GAME_OVER_WINNER_COLOR);
        }
    }
}
