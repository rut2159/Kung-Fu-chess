-- Creates the schema on first run.
--
-- IF NOT EXISTS keeps this safe to run on every startup, so an existing
-- database with real data is left untouched.
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    username      TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    rating        INTEGER NOT NULL DEFAULT 1200
);

-- One row per game actually played (both seats filled at least once).
-- winner/ended_at stay NULL while the game is still in progress.
CREATE TABLE IF NOT EXISTS games (
    id             BIGSERIAL PRIMARY KEY,
    room_id        TEXT NOT NULL,
    white_username TEXT NOT NULL,
    black_username TEXT NOT NULL,
    winner         TEXT,
    started_at     TIMESTAMPTZ NOT NULL,
    ended_at       TIMESTAMPTZ
);

-- room_id is denormalised alongside game_id so a room's history can be
-- queried without a join - GameRoom already has the room id on hand for
-- every move, and this is the query the "reload history on refresh" endpoint
-- actually needs to run.
CREATE TABLE IF NOT EXISTS move_history (
    id           BIGSERIAL PRIMARY KEY,
    game_id      BIGINT NOT NULL REFERENCES games(id),
    room_id      TEXT NOT NULL,
    seq          INTEGER NOT NULL,
    color        TEXT NOT NULL,
    notation     TEXT NOT NULL,
    time_label   TEXT NOT NULL,
    timestamp_ms BIGINT NOT NULL
);
