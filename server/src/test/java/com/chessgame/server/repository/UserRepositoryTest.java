package com.chessgame.server.repository;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uses a real (embedded) Postgres database rather than a mock - the whole
 * point of this class is to prove the actual SQL is correct, which a mock
 * can never verify.
 */
class UserRepositoryTest {

    private static EmbeddedPostgres postgres;

    private UserRepository userRepository;

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
        userRepository = new UserRepository(JdbcClient.create(postgres.getPostgresDatabase()));
    }

    @AfterEach
    void cleanUp() throws Exception {
        EmbeddedDatabase.reset(postgres);
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
