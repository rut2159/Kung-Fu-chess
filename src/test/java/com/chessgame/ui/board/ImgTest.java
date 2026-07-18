package com.chessgame.ui.board;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ImgTest {

    @Test
    void wrap_getReturnsSameInstance() {
        BufferedImage raw = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Img img = Img.wrap(raw);
        assertSame(raw, img.get());
    }

    @Test
    void fillRect_paintsInsideArea() {
        BufferedImage raw = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Img img = Img.wrap(raw);
        img.fillRect(10, 10, 20, 20, Color.RED);
        assertEquals(rgb(Color.RED), rgb(raw.getRGB(15, 15)));
    }

    @Test
    void fillRect_doesNotPaintOutsideArea() {
        BufferedImage raw = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Img img = Img.wrap(raw);
        img.fillRect(10, 10, 20, 20, Color.RED);
        assertEquals(0, raw.getRGB(0, 0));
    }

    @Test
    void copy_isIndependentFromOriginal() {
        BufferedImage src = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        Img srcImg = Img.wrap(src);
        srcImg.fillRect(0, 0, 30, 30, Color.BLUE);

        Img copy = srcImg.copy();
        assertNotSame(src, copy.get());
        assertEquals(src.getWidth(), copy.get().getWidth());
        assertEquals(src.getHeight(), copy.get().getHeight());
        assertEquals(rgb(Color.BLUE), rgb(copy.get().getRGB(5, 5)));

        copy.fillRect(0, 0, 30, 30, Color.GREEN);
        assertEquals(rgb(Color.BLUE), rgb(src.getRGB(5, 5)), "המקור לא אמור להשתנות");
        assertEquals(rgb(Color.GREEN), rgb(copy.get().getRGB(5, 5)), "העותק כן השתנה");
    }

    @Test
    void drawRect_leavesCenterUntouched() {
        BufferedImage canvas = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Img img = Img.wrap(canvas);
        img.drawRect(5, 5, 20, 20, Color.WHITE, 2);
        assertEquals(0, canvas.getRGB(15, 15), "מרכז-המלבן אמור להישאר ריק - זו מסגרת, לא מילוי");
    }

    @Test
    void drawRect_paintsTheBorder() {
        BufferedImage canvas = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Img img = Img.wrap(canvas);
        img.drawRect(5, 5, 20, 20, Color.WHITE, 2);

        boolean borderColored = false;
        for (int x = 5; x < 25; x++) {
            if (rgb(canvas.getRGB(x, 6)) == rgb(Color.WHITE)) {
                borderColored = true;
                break;
            }
        }
        assertTrue(borderColored, "אמור להיות פיקסל-לבן קרוב-לקצה-העליון של המסגרת");
    }

    @Test
    void drawOn_pastesAtGivenLocation() {
        BufferedImage bg = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Img bgImg = Img.wrap(bg);
        bgImg.fillRect(0, 0, 20, 20, Color.BLACK);

        BufferedImage fg = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        Img fgImg = Img.wrap(fg);
        fgImg.fillRect(0, 0, 5, 5, Color.YELLOW);

        fgImg.drawOn(bgImg, 3, 3);

        assertEquals(rgb(Color.YELLOW), rgb(bg.getRGB(5, 5)), "התמונה-הקטנה הודבקה במיקום הנכון");
        assertEquals(rgb(Color.BLACK), rgb(bg.getRGB(0, 0)), "מחוץ-לאזור-ההדבקה לא השתנה");
    }

    @Test
    void drawOn_throwsWhenPatchExceedsDestinationBounds() {
        BufferedImage bg = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Img bgImg = Img.wrap(bg);

        BufferedImage fg = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        Img fgImg = Img.wrap(fg);

        assertThrows(IllegalArgumentException.class, () -> fgImg.drawOn(bgImg, 18, 18));
    }

    @Test
    void putTextCentered_doesNotThrow() {
        BufferedImage canvas = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        Img img = Img.wrap(canvas);
        assertDoesNotThrow(() -> img.putTextCentered("HI", 100, 50, 2.0f, Color.WHITE));
    }

    private int rgb(Color c) {
        return c.getRGB() & 0x00FFFFFF;
    }

    private int rgb(int argb) {
        return argb & 0x00FFFFFF;
    }
}
