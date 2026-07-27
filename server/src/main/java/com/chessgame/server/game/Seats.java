package com.chessgame.server.game;

import com.chessgame.model.Piece;

import java.util.Optional;

public final class Seats {

    public enum Role {
        WHITE(Piece.Color.WHITE),
        BLACK(Piece.Color.BLACK),
        VIEWER(null);

        private final Piece.Color color;

        Role(Piece.Color color) {
            this.color = color;
        }

        public boolean owns(Piece.Color pieceColor) {
            return color != null && color == pieceColor;
        }

        public Piece.Color color() {
            return color;
        }
    }

    private volatile String whiteUsername;
    private volatile String blackUsername;

    public Role take(String username) {
        Role existing = roleOf(username);
        if (existing != Role.VIEWER) {
            return existing;
        }
        if (whiteUsername == null) {
            whiteUsername = username;
            return Role.WHITE;
        }
        if (blackUsername == null) {
            blackUsername = username;
            return Role.BLACK;
        }
        return Role.VIEWER;
    }

    public void release(String username) {
        if (username == null) {
            return;
        }
        if (username.equals(whiteUsername)) {
            whiteUsername = null;
        } else if (username.equals(blackUsername)) {
            blackUsername = null;
        }
    }

    public Role roleOf(String username) {
        if (username == null) {
            return Role.VIEWER;
        }
        if (username.equals(whiteUsername)) {
            return Role.WHITE;
        }
        if (username.equals(blackUsername)) {
            return Role.BLACK;
        }
        return Role.VIEWER;
    }

    public Optional<String> whiteUsername() {
        return Optional.ofNullable(whiteUsername);
    }

    public Optional<String> blackUsername() {
        return Optional.ofNullable(blackUsername);
    }

    public boolean bothSeatsFilled() {
        return whiteUsername != null && blackUsername != null;
    }
}
