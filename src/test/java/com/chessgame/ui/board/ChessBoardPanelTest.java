package com.chessgame.ui.board;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ChessBoardPanelTest {

    @Test
    void constructor_setsBlackBackground() {
        ChessBoardPanel panel = new ChessBoardPanel();
        assertEquals(Color.BLACK, panel.getBackground());
    }

    @Test
    void paintComponent_drawsImageAtGivenOffset() {
        ChessBoardPanel panel = new ChessBoardPanel();
        panel.setSize(200, 200);

        BufferedImage boardImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg = boardImage.createGraphics();
        bg.setColor(Color.RED);
        bg.fillRect(0, 0, 50, 50);
        bg.dispose();

        panel.setBoardImage(boardImage, 30, 40);

        BufferedImage fakeScreen = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = fakeScreen.createGraphics();
        panel.paintComponent(g);
        g.dispose();

        assertEquals(rgb(Color.RED), rgb(fakeScreen.getRGB(35, 45)),
                "פיקסל בתוך אזור-הלוח (offset 30,40 + 5,5 בתוך) אמור להיות אדום");
    }

    @Test
    void paintComponent_doesNotPaintOutsideOffsetArea() {
        ChessBoardPanel panel = new ChessBoardPanel();
        panel.setSize(200, 200);

        BufferedImage boardImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg = boardImage.createGraphics();
        bg.setColor(Color.RED);
        bg.fillRect(0, 0, 50, 50);
        bg.dispose();

        panel.setBoardImage(boardImage, 30, 40);

        BufferedImage fakeScreen = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = fakeScreen.createGraphics();
        panel.paintComponent(g);
        g.dispose();

        assertNotEquals(rgb(Color.RED), rgb(fakeScreen.getRGB(0, 0)),
                "פיקסל-רחוק (0,0) לא-אמור להיות אדום - מחוץ לאזור-הלוח");
    }

    @Test
    void paintComponent_withNoImageSet_doesNotThrow() {
        ChessBoardPanel panel = new ChessBoardPanel();
        panel.setSize(200, 200);

        BufferedImage fakeScreen = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = fakeScreen.createGraphics();
        assertDoesNotThrow(() -> panel.paintComponent(g));
        g.dispose();
    }

    @Test
    void setBoardImage_replacingImage_paintsNewImageNotOld() {
        ChessBoardPanel panel = new ChessBoardPanel();
        panel.setSize(200, 200);

        BufferedImage redImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = redImage.createGraphics();
        g1.setColor(Color.RED);
        g1.fillRect(0, 0, 50, 50);
        g1.dispose();
        panel.setBoardImage(redImage, 0, 0);

        BufferedImage blueImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = blueImage.createGraphics();
        g2.setColor(Color.BLUE);
        g2.fillRect(0, 0, 50, 50);
        g2.dispose();
        panel.setBoardImage(blueImage, 0, 0);

        BufferedImage fakeScreen = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = fakeScreen.createGraphics();
        panel.paintComponent(g);
        g.dispose();

        assertEquals(rgb(Color.BLUE), rgb(fakeScreen.getRGB(10, 10)), "אמורה-להיות-מצוירת התמונה-החדשה, לא הישנה");
    }

    private int rgb(Color c) {
        return c.getRGB() & 0x00FFFFFF;
    }

    private int rgb(int argb) {
        return argb & 0x00FFFFFF;
    }
}
