package com.chessgame.server.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges "authentication" (proving who you are, done once at /api/login with
 * a password) and "identity for this WebSocket connection" (used repeatedly
 * afterward). Without this, a client could just put ?username=anyone in the
 * URL and the server would believe it with no password check at all - this
 * closes that gap: the URL only ever carries an unguessable token, and the
 * server looks up the real username from ITS OWN record of who logged in,
 * never from what the client claims.
 */
@Service
public class SessionTokenService {

    private final Map<String, String> usernameByToken = new ConcurrentHashMap<>();

    public String issueToken(String username) {
        String token = UUID.randomUUID().toString();
        usernameByToken.put(token, username);
        return token;
    }

    public Optional<String> resolveUsername(String token) {
        return Optional.ofNullable(usernameByToken.get(token));
    }
}
