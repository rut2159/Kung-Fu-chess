package com.chessgame.server.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uses a real (in-memory) SQLite database rather than a mock - the whole
 * point of this class is to prove the actual SQL is correct, which a mock
 * can never verify. A SingleConnectionDataSource is required because
 * SQLite's :memory: database only exists for as long as ONE connection
 * to it stays open.
 */
class UserRepositoryTest {

    private SingleConnectionDataSource dataSource;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        try (Statement statement = dataSource.getConnection().createStatement()) {
            statement.execute(
                    "CREATE TABLE users (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  username TEXT UNIQUE NOT NULL," +
                    "  password_hash TEXT NOT NULL," +
                    "  rating INTEGER NOT NULL DEFAULT 1200" +
                    ")"
            );
        }
        userRepository = new UserRepository(JdbcClient.create(dataSource));
    }

    @AfterEach
    void tearDown() {
        dataSource.destroy();
    }

    @Test
    void findByUsername_returnsEmpty_whenNoSuchUser() {
        assertTrue(userRepository.findByUsername("nobody").isEmpty());
    }

    @Test
    void insertThenFindByUsername_returnsTheSameUser() {
        userRepository.insert("alice", "some-hash");

        User found = userRepository.findByUsername("alice").orElseThrow();

        assertEquals("alice", found.username());
        assertEquals("some-hash", found.passwordHash());
        assertEquals(1200, found.rating(), "new users must start at rating 1200");
    }

    @Test
    void insert_rejectsADuplicateUsername() {
        userRepository.insert("alice", "hash-one");

        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> userRepository.insert("alice", "hash-two"),
                "the UNIQUE constraint on username should reject a second insert"
        );
    }

    @Test
    void updateRating_changesOnlyTheRatingColumn() {
        userRepository.insert("alice", "some-hash");

        userRepository.updateRating("alice", 1350);

        User found = userRepository.findByUsername("alice").orElseThrow();
        assertEquals(1350, found.rating());
        assertEquals("some-hash", found.passwordHash(), "updateRating must not touch the password hash");
    }
}
