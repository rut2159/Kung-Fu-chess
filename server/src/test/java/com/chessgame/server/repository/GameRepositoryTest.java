package com.chessgame.server.repository;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Real (embedded) Postgres, not a mock - this is what proves startGame's
 * RETURNING id and completeGame's UPDATE are actually correct SQL.
 */
class GameRepositoryTest {

    private static EmbeddedPostgres postgres;

    private JdbcClient jdbcClient;
    private GameRepository gameRepository;

    private record GameRow(String roomId, String whiteUsername, String blackUsername,
                            String winner, Instant startedAt, Instant endedAt) {
    }

    @BeforeAll
    static void startDatabase() throws Exception {
        postgres = EmbeddedDatabase.start();
    }

    @AfterAll
    static void stopDatabase() throws IOException {
        postgres.close();
    }

    @BeforeEach
    void setUp() {
        jdbcClient = JdbcClient.create(postgres.getPostgresDatabase());
        gameRepository = new GameRepository(jdbcClient);
    }

    @AfterEach
    void cleanUp() throws Exception {
        EmbeddedDatabase.reset(postgres);
    }

    private GameRow findGame(long gameId) {
        return jdbcClient.sql("""
                        SELECT room_id, white_username, black_username, winner, started_at, ended_at
                        FROM games WHERE id = :id
                        """)
                .param("id", gameId)
                .query(GameRow.class)
                .single();
    }

    @Test
    void startGame_insertsARowWithBothUsernamesAndNoResultYet() {
        long gameId = gameRepository.startGame("ROOM1", "alice", "bob");

        GameRow row = findGame(gameId);

        assertEquals("ROOM1", row.roomId());
        assertEquals("alice", row.whiteUsername());
        assertEquals("bob", row.blackUsername());
        assertNotNull(row.startedAt());
        assertNull(row.winner(), "an in-progress game must not have a winner yet");
        assertNull(row.endedAt(), "an in-progress game must not have an end time yet");
    }

    @Test
    void completeGame_setsWinnerAndEndedAt() {
        long gameId = gameRepository.startGame("ROOM2", "alice", "bob");

        gameRepository.completeGame(gameId, "WHITE");

        GameRow row = findGame(gameId);
        assertEquals("WHITE", row.winner());
        assertNotNull(row.endedAt());
    }

    @Test
    void completeGame_acceptsANullWinner_forAnAbortedGame() {
        long gameId = gameRepository.startGame("ROOM3", "alice", "bob");

        gameRepository.completeGame(gameId, null);

        GameRow row = findGame(gameId);
        assertNull(row.winner());
        assertNotNull(row.endedAt());
    }
}
