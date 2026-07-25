package com.chessgame.ui.board;

import com.chessgame.model.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.junit.jupiter.api.Assertions.*;

class UiMapperTest {

    private UiMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UiMapper();
    }

    @Test
    void defaultCellSize_is100() {
        assertEquals(100, mapper.getCellSize());
    }

    @Test
    void setCellSize_updatesGetCellSize() {
        mapper.setCellSize(80);
        assertEquals(80, mapper.getCellSize());
    }

    @Test
    void setCellSize_rejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> mapper.setCellSize(0));
    }

    @Test
    void setCellSize_rejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> mapper.setCellSize(-5));
    }

    @Test
    void cellToPixel_position_multipliesByCellSize() {
        mapper.setCellSize(50);
        Point p = mapper.cellToPixel(new Position(2, 3));
        assertEquals(150, p.x); // col=3 * 50
        assertEquals(100, p.y); // row=2 * 50
    }

    @Test
    void cellToPixel_fractional_interpolatesCorrectly() {
        mapper.setCellSize(100);
        Point p = mapper.cellToPixel(2.5, 1.0);
        assertEquals(100, p.x);
        assertEquals(250, p.y);
    }

    @Test
    void cellToPixel_position_and_fractionalWholeNumber_agree() {
        mapper.setCellSize(64);
        Point viaPosition = mapper.cellToPixel(new Position(3, 5));
        Point viaFractional = mapper.cellToPixel(3.0, 5.0);
        assertEquals(viaPosition, viaFractional);
    }

    @Test
    void pixelToCell_convertsBack() {
        mapper.setCellSize(100);
        Position pos = mapper.pixelToCell(250, 130);
        assertEquals(1, pos.row());
        assertEquals(2, pos.col());
    }

    @Test
    void pixelToCell_negativeX_returnsNull() {
        assertNull(mapper.pixelToCell(-1, 10));
    }

    @Test
    void pixelToCell_negativeY_returnsNull() {
        assertNull(mapper.pixelToCell(10, -1));
    }
}
