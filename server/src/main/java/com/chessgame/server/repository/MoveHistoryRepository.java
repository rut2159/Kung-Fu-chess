package com.chessgame.server.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MoveHistoryRepository {

    private final JdbcClient jdbcClient;

    public MoveHistoryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void append(long gameId, String roomId, int seq, String color, String notation,
                        String timeLabel, long timestampMs) {
        jdbcClient.sql("""
                        INSERT INTO move_history (game_id, room_id, seq, color, notation, time_label, timestamp_ms)
                        VALUES (:gameId, :roomId, :seq, :color, :notation, :timeLabel, :timestampMs)
                        """)
                .param("gameId", gameId)
                .param("roomId", roomId)
                .param("seq", seq)
                .param("color", color)
                .param("notation", notation)
                .param("timeLabel", timeLabel)
                .param("timestampMs", timestampMs)
                .update();
    }
}
