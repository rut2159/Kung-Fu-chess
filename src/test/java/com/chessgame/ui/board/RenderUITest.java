package com.chessgame.ui.board;

import com.chessgame.engine.GameSnapshot;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RenderUITest {

    private UiMapper mapper;
    private RenderUI renderUI;

    @BeforeEach
    void setUp() {
        mapper = new UiMapper();
        renderUI = new RenderUI(mapper, "Alice", "Bob");
    }

    @Test
    void renderNewFrame_emptyBoard_matchesCellSizeTimesBoardSize() {
        mapper.setCellSize(40);
        GameSnapshot snapshot = new GameSnapshot(8, 8, new ArrayList<>(), null, false, null);

        BufferedImage image = renderUI.renderNewFrame(snapshot);

        assertEquals(320, image.getWidth());  // 8 * 40
        assertEquals(320, image.getHeight());
    }

    @Test
    void renderNewFrame_differentCellSize_scalesOutputAccordingly() {
        mapper.setCellSize(20);
        GameSnapshot snapshot = new GameSnapshot(8, 8, new ArrayList<>(), null, false, null);

        BufferedImage image = renderUI.renderNewFrame(snapshot);

        assertEquals(160, image.getWidth());  // 8 * 20
        assertEquals(160, image.getHeight());
    }

    @Test
    void renderNewFrame_withSelectedCell_doesNotThrow() {
        mapper.setCellSize(40);
        GameSnapshot snapshot = new GameSnapshot(8, 8, new ArrayList<>(), new Position(3, 3), false, null);

        assertDoesNotThrow(() -> renderUI.renderNewFrame(snapshot));
    }

    @Test
    void renderNewFrame_withPieces_doesNotThrow_andKeepsCorrectDimensions() {
        mapper.setCellSize(40);
        List<GameSnapshot.PieceView> pieces = new ArrayList<>();
        pieces.add(new GameSnapshot.PieceView("p1", Piece.Color.WHITE, Piece.Kind.ROOK,
                new Position(7, 0), Piece.State.IDLE));
        pieces.add(new GameSnapshot.PieceView("p2", Piece.Color.BLACK, Piece.Kind.KING,
                new Position(0, 4), Piece.State.IDLE));
        GameSnapshot snapshot = new GameSnapshot(8, 8, pieces, null, false, null);

        BufferedImage image = assertDoesNotThrow(() -> renderUI.renderNewFrame(snapshot));

        assertEquals(320, image.getWidth());
        assertEquals(320, image.getHeight());
    }

    @Test
    void renderNewFrame_pieceWithPremove_tintsCellBlue() {
        mapper.setCellSize(40);
        List<GameSnapshot.PieceView> pieces = new ArrayList<>();
        GameSnapshot.PieceView premoveRook = new GameSnapshot.PieceView(
                "p1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0), Piece.State.COOLDOWN_LONG, 0.3
        ).withPremove(true);
        pieces.add(premoveRook);
        GameSnapshot snapshot = new GameSnapshot(8, 8, pieces, null, false, null);

        BufferedImage image = assertDoesNotThrow(() -> renderUI.renderNewFrame(snapshot));

        // הפינה העליונה-שמאלית של התא לרוב לא מכוסה ע"י ספרייט הכלי, אבל כן ע"י צביעת ה-premove
        Point pixel = mapper.cellToPixel(new Position(7, 0));
        int rgb = image.getRGB(pixel.x + 1, pixel.y + 1);
        int blue = rgb & 0xFF;
        int red = (rgb >> 16) & 0xFF;
        assertTrue(blue > red, "הפינה אמורה להיראות כחלחלה מהצביעה של ה-premove");
    }

    @Test
    void renderNewFrame_gameOver_paintsAVisibleBand() {
        mapper.setCellSize(40);
        GameSnapshot snapshot = new GameSnapshot(8, 8, new ArrayList<>(), null, true, Piece.Color.WHITE);

        BufferedImage image = renderUI.renderNewFrame(snapshot);

        int centerAlpha = (image.getRGB(image.getWidth() / 2, image.getHeight() / 2) >> 24) & 0xFF;
        assertTrue(centerAlpha > 200, "אמור להיות פס-רקע-כמעט-אטום במרכז-התמונה, כשהמשחק נגמר");
    }
}
