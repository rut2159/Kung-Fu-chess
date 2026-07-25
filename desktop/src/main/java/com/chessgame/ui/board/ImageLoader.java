package com.chessgame.ui.board;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

final class ImageLoader {
    private ImageLoader() {
    }

    static BufferedImage load(String path, Dimension targetSize, boolean keepAspect) {
        BufferedImage img;
        try (InputStream in = ImageLoader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("Cannot find image on classpath: " + path);
            }
            img = ImageIO.read(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load image: " + path);
        }
        if (img == null) {
            throw new IllegalArgumentException("Unsupported image: " + path);
        }

        return (targetSize == null) ? img : resize(img, targetSize, keepAspect);
    }

    private static BufferedImage resize(BufferedImage img, Dimension targetSize, boolean keepAspect) {
        int tw = targetSize.width, th = targetSize.height;
        int w = img.getWidth(), h = img.getHeight();

        int nw, nh;
        if (keepAspect) {
            double s = Math.min(tw / (double) w, th / (double) h);
            nw = (int) Math.round(w * s);
            nh = (int) Math.round(h * s);
        } else {
            nw = tw;
            nh = th;
        }

        BufferedImage dst = new BufferedImage(
                nw, nh,
                img.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, nw, nh, null);
        g.dispose();
        return dst;
    }
}
