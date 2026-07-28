package com.chessgame.server.repository;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Real (embedded) Postgres, not a mock - move_history.game_id is a real
 * foreign key into games(id), so this also proves append() only succeeds
 * against a game row that actually exists.
 */
class MoveHistoryRepositoryTest {

    private static EmbeddedPostgres postgres;

    private JdbcClient jdbcClient;
    private GameRepository gameRepository;
    private MoveHistoryRepository moveHistoryRepository;

    private record MoveRow(long gameId, String roomId, int seq, String color,
                            String notation, String timeLabel, long timestampMs) {
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
        moveHistoryRepository = new MoveHistoryRepository(jdbcClient);
    }

    @AfterEach
    void cleanUp() throws Exception {
        EmbeddedDatabase.reset(postgres);
    }

    private List<MoveRow> findMoves(String roomId) {
        return jdbcClient.sql("""
                        SELECT game_id, room_id, seq, color, notation, time_label, timestamp_ms
                        FROM move_history WHERE room_id = :roomId ORDER BY seq
                        """)
                .param("roomId", roomId)
                .query(MoveRow.class)
                .list();
    }

    @Test
    void append_insertsAMoveRowTiedToItsGame() {
        long gameId = gameRepository.startGame("ROOM1", "alice", "bob");

        moveHistoryRepository.append(gameId, "ROOM1", 1, "WHITE", "e2e4", "0:01", 1_000L);

        MoveRow row = findMoves("ROOM1").getFirst();
        assertEquals(gameId, row.gameId());
        assertEquals("ROOM1", row.roomId());
        assertEquals(1, row.seq());
        assertEquals("WHITE", row.color());
        assertEquals("e2e4", row.notation());
        assertEquals("0:01", row.timeLabel());
        assertEquals(1_000L, row.timestampMs());
    }

    @Test
    void append_keepsMultipleMovesInSequenceOrder() {
        long gameId = gameRepository.startGame("ROOM2", "alice", "bob");

        moveHistoryRepository.append(gameId, "ROOM2", 1, "WHITE", "e2e4", "0:01", 1_000L);
        moveHistoryRepository.append(gameId, "ROOM2", 2, "BLACK", "e7e5", "0:03", 3_000L);

        List<MoveRow> rows = findMoves("ROOM2");
        assertEquals(2, rows.size());
        assertEquals("e2e4", rows.get(0).notation());
        assertEquals("e7e5", rows.get(1).notation());
    }
}
