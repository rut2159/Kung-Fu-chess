package com.chessgame.server.game;

import java.security.SecureRandom;
import java.util.Locale;

public final class RoomId {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RoomId() {
    }

    public static String generate() {
        StringBuilder id = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            id.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return id.toString();
    }

    public static String normalise(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isWellFormed(String candidate) {
        if (candidate == null || candidate.length() != LENGTH) {
            return false;
        }
        for (int i = 0; i < LENGTH; i++) {
            if (ALPHABET.indexOf(candidate.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }
}
