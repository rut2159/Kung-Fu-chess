package com.chessgame.ui.board;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.io.IOException;

/**
 * Lightweight image‑utility class using only standard JDK APIs.
 */
public class Img {

    private BufferedImage img;

    /* ----------- load & optional resize ----------- */
    public Img read(String path,
                    Dimension targetSize,
                    boolean keepAspect,
                    Object interpolation /*ignored*/) {

        try (java.io.InputStream in = Img.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("Cannot find image on classpath: " + path);
            }
            img = ImageIO.read(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load image: " + path);
        }
        if (img == null) throw new IllegalArgumentException("Unsupported image: " + path);

        if (targetSize != null) {
            int tw = targetSize.width, th = targetSize.height;
            int w = img.getWidth(), h = img.getHeight();

            int nw, nh;
            if (keepAspect) {                                                // :contentReference[oaicite:1]{index=1}
                double s = Math.min(tw / (double) w, th / (double) h);
                nw = (int) Math.round(w * s);
                nh = (int) Math.round(h * s);
            } else { nw = tw; nh = th; }

            BufferedImage dst = new BufferedImage(
                    nw, nh,
                    img.getColorModel().hasAlpha()
                            ? BufferedImage.TYPE_INT_ARGB
                            : BufferedImage.TYPE_INT_RGB);

            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);   // :contentReference[oaicite:2]{index=2}
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            img = dst;
        }
        return this;
    }

    public Img read(String path) { return read(path, null, false, null); }

    /* ----------- wrap an already-decoded image (no disk/classpath read) ----------- */
    public static Img wrap(BufferedImage image) {
        Img wrapped = new Img();
        wrapped.img = image;
        return wrapped;
    }

    /* ----------- draw this image onto another ----------- */
    public void drawOn(Img other, int x, int y) {
        if (img == null || other.img == null)
            throw new IllegalStateException("Both images must be loaded.");

        if (x + img.getWidth()  > other.img.getWidth()
                || y + img.getHeight() > other.img.getHeight())
            throw new IllegalArgumentException("Patch exceeds destination bounds.");

        Graphics2D g = other.img.createGraphics();
        g.setComposite(AlphaComposite.SrcOver);                               // handles alpha channel :contentReference[oaicite:3]{index=3}
        g.drawImage(img, x, y, null);                                        // :contentReference[oaicite:4]{index=4}
        g.dispose();
    }

    /* ----------- annotate with text ----------- */
    public void putText(String txt, int x, int y, float fontSize,
                        Color color, int thickness /*unused in Java2D*/) {

        if (img == null) throw new IllegalStateException("Image not loaded.");

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(color);
        g.setFont(img.getGraphics().getFont().deriveFont(fontSize * 12));     // simple scale
        g.drawString(txt, x, y);                                             // :contentReference[oaicite:5]{index=5}
        g.dispose();
    }

    /* ----------- annotate with text, horizontally+vertically centered on (centerX, centerY) ----------- */
    public void putTextCentered(String text, int centerX, int centerY, float fontSize, Color color) {
        if (img == null) throw new IllegalStateException("Image not loaded.");

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font font = img.getGraphics().getFont().deriveFont(fontSize * 12);
        g.setFont(font);
        g.setColor(color);

        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int x = centerX - textWidth / 2;
        int y = centerY + (metrics.getAscent() - metrics.getDescent()) / 2;

        g.drawString(text, x, y);
        g.dispose();
    }

    /* ----------- display in a Swing window ----------- */
    public void show() {
        if (img == null) throw new IllegalStateException("Image not loaded.");

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Image");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new JLabel(new ImageIcon(img)));                            // :contentReference[oaicite:6]{index=6}
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    /* ----------- draw a filled rectangle (e.g. cell highlight) ----------- */
    public void fillRect(int x, int y, int width, int height, Color color) {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(x, y, width, height);
        g.dispose();
    }

    /* ----------- draw a rectangle outline only (not filled) - e.g. selection frame ----------- */
    public void drawRect(int x, int y, int width, int height, Color color, int thickness) {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        int inset = thickness / 2;
        g.drawRect(x + inset, y + inset, width - thickness, height - thickness);
        g.dispose();
    }

    /* ----------- independent duplicate, safe to draw on without touching the original ----------- */
    public Img copy() {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        BufferedImage duplicate = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        Graphics2D g = duplicate.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return Img.wrap(duplicate);
    }

    /* ----------- access (optional) ----------- */
    public BufferedImage get() { return img; }
}
