package com.chessgame.server.service;

import com.chessgame.model.Piece;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlayerAssignmentService {

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
    }

    private final Map<String, String> usernameBySession = new ConcurrentHashMap<>();
    private final Map<String, Role> roleByUsername = new ConcurrentHashMap<>();
    private volatile String whiteUsername;
    private volatile String blackUsername;

    public synchronized Role assign(String sessionId, String username) {
        usernameBySession.put(sessionId, username);

        Role existing = roleByUsername.get(username);
        if (existing != null) {
            return existing;
        }

        Role role;
        if (whiteUsername == null) {
            whiteUsername = username;
            role = Role.WHITE;
        } else if (blackUsername == null && !username.equals(whiteUsername)) {
            blackUsername = username;
            role = Role.BLACK;
        } else {
            role = Role.VIEWER;
        }
        roleByUsername.put(username, role);
        return role;
    }

    public Role roleForSession(String sessionId) {
        String username = usernameBySession.get(sessionId);
        if (username == null) {
            return Role.VIEWER;
        }
        return roleByUsername.getOrDefault(username, Role.VIEWER);
    }

    public Optional<String> usernameForSession(String sessionId) {
        return Optional.ofNullable(usernameBySession.get(sessionId));
    }
    public synchronized Optional<String> disconnect(String sessionId) {
        String username = usernameBySession.remove(sessionId);
        if (username == null || usernameBySession.containsValue(username)) {
            return Optional.empty();
        }
        return Optional.of(username);
    }

    public boolean hasActiveSession(String username) {
        return usernameBySession.containsValue(username);
    }

    public Role roleForUsername(String username) {
        return roleByUsername.getOrDefault(username, Role.VIEWER);
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
