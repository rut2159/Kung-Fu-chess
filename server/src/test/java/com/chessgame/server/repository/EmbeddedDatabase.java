package com.chessgame.server.repository;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Starts a real (embedded, no Docker daemon required) Postgres and applies
 * the production schema.sql, so repository tests prove the actual SQL -
 * including the move_history -> games foreign key - works against real
 * Postgres, not a mock or a different database engine.
 */
final class EmbeddedDatabase {

    private EmbeddedDatabase() {
    }

    static EmbeddedPostgres start() throws IOException, SQLException {
        EmbeddedPostgres postgres = EmbeddedPostgres.start();
        try (Connection connection = postgres.getPostgresDatabase().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(readSchemaSql());
        } catch (SQLException | RuntimeException e) {
            postgres.close();
            throw e;
        }
        return postgres;
    }

    static void reset(EmbeddedPostgres postgres) throws SQLException {
        try (Connection connection = postgres.getPostgresDatabase().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE move_history, games, users RESTART IDENTITY CASCADE");
        }
    }

    private static String readSchemaSql() {
        try (InputStream in = EmbeddedDatabase.class.getResourceAsStream("/schema.sql")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
