package com.chessgame.server.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<User> findByUsername(String username) {
        return jdbcClient.sql("SELECT id, username, password_hash, rating FROM users WHERE username = :username")
                .param("username", username)
                .query(User.class)
                .optional();
    }

    public void insert(String username, String passwordHash) {
        jdbcClient.sql("INSERT INTO users (username, password_hash, rating) VALUES (:username, :passwordHash, 1200)")
                .param("username", username)
                .param("passwordHash", passwordHash)
                .update();
    }

    public void updateRating(String username, int newRating) {
        jdbcClient.sql("UPDATE users SET rating = :rating WHERE username = :username")
                .param("rating", newRating)
                .param("username", username)
                .update();
    }
}
