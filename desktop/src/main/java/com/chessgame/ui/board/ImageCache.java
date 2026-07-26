package com.chessgame.ui.board;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ImageCache {
    private final Map<String, BufferedImage> cache = new ConcurrentHashMap<>();

    BufferedImage get(String path, int width, int height, boolean keepAspect) {
        String key = path + "@" + width + "x" + height;
        return cache.computeIfAbsent(key,
                k -> new Img().read(path, new Dimension(width, height), keepAspect, null).get());
    }
}
