package com.chessgame.server.repository;

public record User(Long id, String username, String passwordHash, int rating) {
}
