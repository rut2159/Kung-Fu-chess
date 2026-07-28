package com.chessgame.server.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class GameRepository {

    private final JdbcClient jdbcClient;

    public GameRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Called once a room's second seat is taken. Returns the generated id so
     * the caller can later attach move-history rows and a result to it.
     */
    public long startGame(String roomId, String whiteUsername, String blackUsername) {
        return jdbcClient.sql("""
                        INSERT INTO games (room_id, white_username, black_username, started_at)
                        VALUES (:roomId, :whiteUsername, :blackUsername, now())
                        RETURNING id
                        """)
                .param("roomId", roomId)
                .param("whiteUsername", whiteUsername)
                .param("blackUsername", blackUsername)
                .query(Long.class)
                .single();
    }

    /**
     * @param winner "WHITE", "BLACK", or null for a game that ended without a winner
     *               (e.g. aborted).
     */
    public void completeGame(long gameId, String winner) {
        jdbcClient.sql("UPDATE games SET winner = :winner, ended_at = now() WHERE id = :id")
                .param("winner", winner)
                .param("id", gameId)
                .update();
    }
}
