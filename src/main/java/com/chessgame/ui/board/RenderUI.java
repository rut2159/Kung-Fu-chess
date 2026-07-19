package com.chessgame.ui.board;

import com.chessgame.engine.GameSnapshot;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public final class RenderUI {

    private static final String BOARD_IMAGE_PATH = "/board.png";
    private static final String PIECES_BASE_PATH = "/pieces/";
    private static final int FRAME_COUNT = 5;
    private static final Color SELECTED_CELL_BORDER_COLOR = new Color(255, 255, 255, 230);
    private static final int SELECTED_CELL_BORDER_THICKNESS = 4;
    private static final Color COOLDOWN_HIGHLIGHT_COLOR = new Color(212, 175, 55, 160);
    private static final Color PREMOVE_HIGHLIGHT_COLOR = new Color(66, 133, 244, 160);
    private static final Color COORDINATE_LABEL_COLOR = new Color(255, 255, 255, 210);
    private static final Color GAME_OVER_BACKGROUND = new Color(0, 0, 0, 235);
    private static final Color GAME_OVER_TITLE_COLOR = Color.WHITE;
    private static final Color GAME_OVER_WINNER_COLOR = new Color(212, 175, 55);

    private static final Map<String, BufferedImage> IMAGE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private final UiMapper mapper;
    private final String whitePlayerName;
    private final String blackPlayerName;

    public RenderUI(UiMapper mapper, String whitePlayerName, String blackPlayerName) {
        this.mapper = mapper;
        this.whitePlayerName = whitePlayerName;
        this.blackPlayerName = blackPlayerName;
    }

    public BufferedImage renderNewFrame(GameSnapshot snapshot) {
        int cellSize = mapper.getCellSize();
        int imageWidth = cellSize * snapshot.width();
        int imageHeight = cellSize * snapshot.height();

        BufferedImage cachedBackground = cachedImage(BOARD_IMAGE_PATH, imageWidth, imageHeight, false);
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

    private BufferedImage cachedImage(String path, int width, int height, boolean keepAspect) {
        String key = path + "@" + width + "x" + height;
        return IMAGE_CACHE.computeIfAbsent(key,
                k -> new Img().read(path, new Dimension(width, height), keepAspect, null).get());
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

    /** תא-כלי עם premove ממתין - צביעה כחולה שקופה על כל המשבצת (בסגנון-דומה לצביעת הקירור הצהובה). */
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
        String path = spritePath(piece);
        if (path == null) {
            return;
        }

        BufferedImage cachedPiece = cachedImage(path, cellSize, cellSize, true);
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

    private String spritePath(GameSnapshot.PieceView piece) {
        String stateFolder = stateFolder(piece.state());
        if (stateFolder == null) {
            return null;
        }
        String code = pieceFolderCode(piece.color(), piece.kind());
        int frame = currentFrameIndex(piece.state());
        return PIECES_BASE_PATH + code + "/states/" + stateFolder + "/sprites/" + frame + ".png";
    }

    private String stateFolder(Piece.State state) {
        switch (state) {
            case IDLE: return "idle";
            case MOVING: return "move";
            case AIRBORNE: return "jump";
            case COOLDOWN_LONG: return "long_rest";
            case COOLDOWN_SHORT: return "short_rest";
            case CAPTURED: return null;
            default: return null;
        }
    }

    private String pieceFolderCode(Piece.Color color, Piece.Kind kind) {
        String kindLetter;
        switch (kind) {
            case KING: kindLetter = "K"; break;
            case QUEEN: kindLetter = "Q"; break;
            case ROOK: kindLetter = "R"; break;
            case BISHOP: kindLetter = "B"; break;
            case KNIGHT: kindLetter = "N"; break;
            case PAWN: kindLetter = "P"; break;
            default: throw new IllegalArgumentException("Unknown piece kind: " + kind);
        }
        String colorLetter = (color == Piece.Color.WHITE) ? "W" : "B";
        return kindLetter + colorLetter;
    }

    private int currentFrameIndex(Piece.State state) {
        if (state != Piece.State.MOVING && state != Piece.State.AIRBORNE) {
            return 1;
        }
        int fps = framesPerSecondFor(state);
        long elapsedFrames = (System.currentTimeMillis() * fps) / 1000;
        return (int) (elapsedFrames % FRAME_COUNT) + 1;
    }

    private int framesPerSecondFor(Piece.State state) {
        return state == Piece.State.AIRBORNE ? 8 : 12;
    }
}
