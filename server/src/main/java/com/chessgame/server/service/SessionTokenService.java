package com.chessgame.server.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
