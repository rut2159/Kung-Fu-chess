-- Creates the schema on first run.
--
-- Until now nothing in production code created this table. It existed only
-- because a database file had been created by hand at some point and carried
-- along ever since, which meant the application could not start on a machine
-- that did not already have that file.
--
-- IF NOT EXISTS keeps this safe to run on every startup, so an existing
-- database with real users is left untouched.
CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    rating        INTEGER NOT NULL DEFAULT 1200
);
