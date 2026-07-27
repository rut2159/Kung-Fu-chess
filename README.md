# Kung Fu Chess

A real-time, no-turns variant of chess: both players move **simultaneously and independently**. There's no "your turn / my turn" — every piece moves the instant you tell it to, subject only to its own cooldown after moving. Captures resolve based on *actual arrival time*, not on what the board looked like when you clicked, so a piece can be attacked and still escape if it moves away before the attacker lands.

Two front ends share one game engine:

- 🖥️ **Desktop app** (Java Swing) — local, same-machine, pass-and-play.
- 🌐 **Web client + server** (Spring Boot, STOMP over WebSocket) — play over a network, with accounts, ELO ratings, rooms, matchmaking, and persistence.

## Highlights

- **Real-time engine, not turn-based.** A single heartbeat (`GameEngine.wait(ms)`) drives cooldowns, in-flight motion, and arrivals — no move queue waiting on a "turn."
- **Arrival-time capture resolution.** The engine never predicts an outcome it can't yet know; a capture is only real once the attacking piece actually lands on an occupied square.
- **Premoves.** Clicking a piece mid-cooldown queues the move instead of rejecting it; it fires the instant the cooldown clears.
- **ELO-based matchmaking.** Hit "Play" and get paired automatically with an opponent rated within ±100 — or create/join a room by a 6-character code instead.
- **Reconnect-safe.** A dropped connection starts a visible 20-second forfeit countdown instead of ending the game outright; rejoining in time cancels it and hands the same seat back.
- **Full activity logging**, client and server side, with secrets (passwords, session tokens) redacted before anything touches disk.

## Architecture

A 5-module Maven build. The core idea: the game engine has **zero knowledge** of how it's being displayed or transported — desktop and server are two independent, replaceable shells around the same rules.

```
kung-fu-chess-parent (pom)
├── core     the game engine itself: board, pieces, rules, real-time
│            arbiter, event bus. No UI, no networking, no dependency
│            on anything else in this project.
├── assets   shared static resources (board image, piece sprites, sound
│            clips) used by both the desktop app and the web client.
├── desktop  Java Swing client. Depends on core + assets.
├── client   the web front end: plain HTML/CSS/JS (no build step, no
│            framework). Depends on assets for sprite/sound files.
└── server   Spring Boot backend. Depends on core, assets, and client.
             Serves the web client's static files, exposes a STOMP +
             REST API, and owns accounts, ratings, rooms, and persistence.
```

**Why this split:**

- `core` never needs to know who's listening. It just runs a game — you call `requestMove`, `requestJump`, and `wait(ms)`, and it publishes events (`MoveMadeEvent`, `ScoreChangedEvent`, `GameOverEvent`, ...) on an in-process event bus. Desktop and server each subscribe for their own purposes (sound, move logs, network broadcast).
- `assets` exists so sprite/sound files aren't duplicated between the desktop jar and the server's web-serving classpath.
- `client` is a separate module from `server` on purpose: it's plain static files with no Java in them, keeping "the thing that speaks HTTP/STOMP" cleanly apart from "the thing a browser downloads and runs," even though the server is what serves it.

## What's implemented

**Local play (desktop)**
- Full real-time rule engine: legal moves per piece, cooldowns, collision/arrival resolution, premoves, promotion, scoring, game-over detection.
- Swing UI — board rendering from sprite sheets, drag-to-move, click-to-select, double-click to jump, per-player move history panel, sound effects.

**Networked play (server + web client)**
- Registration/login (SQLite-backed, BCrypt-hashed passwords).
- **Play button** — matchmaking by ELO rating within ±100; times out after a minute with a clear "couldn't find an opponent" message if no one's around.
- **Rooms** — create a room and share its 6-character code, or join one someone else created. Any number of rooms can run concurrently, each with its own independent game.
- First two distinct logged-in users to join a room become White and Black; everyone after that is a spectator.
- Session tokens issued at login and required on every join — a client can never claim to be a different username than the one it authenticated as.
- ELO-style rating (starting at 1200), updated automatically when a game ends, including forfeits.
- Live move history broadcast to everyone in the room, and reloaded from the server on reconnect/refresh so it's never lost.
- A single-threaded queue per room serializes every state-changing operation (joins, moves, jumps, the periodic clock tick) — the scheduled game clock and incoming player commands can never race each other.
- Full client- and server-side activity logging, with password/token fields redacted before they ever reach a log file.
- Fully responsive layout — board and side panels resize to fit any window without the page itself scrolling.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 26 (source/target) |
| Build | Maven (multi-module reactor) |
| Desktop UI | Java Swing |
| Server | Spring Boot 4.1, STOMP over WebSocket (SockJS) + REST |
| Persistence | SQLite (via Spring's `JdbcClient`), schema in `schema.sql` |
| Auth | BCrypt password hashing (`spring-security-crypto` only — not the full Spring Security framework) |
| Web client | Plain HTML/CSS/JS — no framework, no build step |
| Testing | JUnit 5, Mockito |

## Running it

### Desktop
Run `com.chessgame.Main` from the `desktop` module — opens a local, pass-and-play window for two players on the same machine.

### Server + web client
1. `mvn clean install` from the project root.
2. Run `ServerApplication` (in `server`).
3. Open `http://localhost:8080/` in a browser, register, then log in.
4. Click **Play** to get matched automatically, or **Room** to create/join by code. Open a second browser (or an incognito window) and log in as a different user to fill the second seat.

## Testing

`mvn test` from the root runs every module's suite:
- `core` — the largest suite: rule engine, real-time arbiter/collision/cooldown behavior, the event bus, snapshot/DTO shape, and regression tests for arrival-timing and premove-event edge cases.
- `desktop` — UI-adjacent logic (controller, board mapping, move history formatting) plus an end-to-end "a real click reaches the sound/log subscribers" test.
- `server` — unit tests (Mockito) for auth, rating, rooms, seating, matchmaking, and the STOMP/REST controllers, plus a real (in-memory SQLite) integration test for the user repository.

## Project layout reference

```
core/src/main/java/com/chessgame/
├── model/        Board, Piece, Position, GameState
├── rules/        RuleEngine, per-piece move rules, MoveReason
├── realtime/     RealTimeArbiter, cooldowns, collisions, arrival resolution
├── engine/       GameEngine, move requests, premoves, scoring, snapshots
├── bus/          SimpleEventBus + event types
├── logging/      Bus subscribers for console logging
└── io/           Board text-format parser/printer

server/src/main/java/com/chessgame/server/
├── controller/   STOMP + REST endpoints (auth, rooms, matchmaking, game commands)
├── game/         RoomRegistry, GameRoom, Seats, RoomId, Matchmaker, Topics
├── service/      AuthService, RatingService, SessionTokenService, GameTicker
├── repository/   UserRepository (JdbcClient)
├── dto/          Wire-format messages
├── logging/      Redacted client/server activity logging
└── config/       WebSocket + static resource configuration
```
