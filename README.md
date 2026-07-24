# Kung Fu Chess

A real-time, no-turns variant of chess: both players move simultaneously and independently. There's no "your turn / my turn" — every piece moves the instant you tell it to, subject only to its own cooldown after moving. Captures resolve based on *actual arrival time*, not on what the board looked like when you clicked, so a piece can be attacked and still escape if it moves away before the attacker lands.

The project has two front ends sharing one game engine:
- A **desktop app** (Java Swing) for local, same-machine play.
- A **web client + server** (Spring Boot, STOMP over WebSocket) for playing over a network, with accounts, ratings, and persistence.

## Architecture

The project is a 5-module Maven build. The core idea: the game engine has **zero knowledge** of how it's being displayed or transported — desktop and server are two independent, replaceable shells around the same rules.

```
kung-fu-chess-parent (pom)
├── core     - the game engine itself: board, pieces, rules, real-time
│              arbiter, event bus. No UI, no networking, no dependencies
│              on anything else in this project.
├── assets   - shared static resources (board image, piece sprites, sound
│              clips) used by both the desktop app and the web client.
├── desktop  - Java Swing client. Depends on core + assets.
├── client   - the web front end: plain HTML/CSS/JS (no build step, no
│              framework). Depends on assets (for the sprite/sound files,
│              bundled and served the same way core is).
└── server   - Spring Boot backend. Depends on core, assets, and client.
               Serves the web client's static files, exposes a STOMP
               WebSocket API, and owns accounts/ratings/persistence.
```

### Why this split

- **`core` has no idea a server or a Swing window exists.** It just runs a game: you call `requestMove`, `requestJump`, and `wait(ms)` to advance time, and it publishes events (`MoveMadeEvent`, `ScoreChangedEvent`, `GameOverEvent`, ...) on an in-process event bus. Both desktop and server subscribe to those events for their own purposes (sound, move logs, broadcasting to the network) — `core` never needs to know who's listening.
- **`assets` exists so sprite/sound files aren't duplicated** between the desktop jar and the server's web-serving classpath.
- **`client` is a separate module, not folder, from `server`** on purpose: it's plain static files (no Java), and keeping it out of `server`'s own `src/` keeps "the thing that talks HTTP/STOMP" and "the thing a browser downloads and runs" as clearly separate concerns, even though the server is what serves it.

## Core engine highlights

- **Real-time, not turn-based.** `GameEngine.wait(milliseconds)` is the heartbeat — call it repeatedly (a render loop on desktop, a scheduled tick on the server) to advance cooldowns, in-flight motions, and arrivals.
- **Capture is decided at arrival, not at request time.** If you attack a square and the target moves away before your piece lands, it's correctly logged as a non-capturing move — the engine never "predicts" an outcome it can't yet know.
- **Premoves.** Clicking a piece while it's still cooling down queues a premove instead of rejecting it outright; it fires automatically the instant the cooldown clears, going through the exact same acceptance/event path as a manually-requested move.
- **In-process event bus** (`SimpleEventBus`) decouples the engine from anything reacting to it. Current subscribers: move logging, score-change logging, game-over logging, sound (desktop), and network broadcasting (server).

## What's implemented

**Local play (desktop)**
- Full real-time rule engine: legal moves per piece, cooldowns, collision/arrival resolution, premoves, promotion, scoring, game-over detection.
- Swing UI: board rendering from sprite sheets, drag-to-move, click-to-select, double-click to jump (reset a piece's own cooldown), move history panel per player, sound effects.

**Networked play (server + web client)**
- Spring Boot server communicating over STOMP/WebSocket (SockJS fallback).
- Login page → game page flow, with registration and login (SQLite-backed, BCrypt-hashed passwords, no plaintext ever stored).
- Session tokens issued at login and used to identify a client's `/app/join` request — a client can't claim to be a different username than the one it actually authenticated as.
- First two distinct logged-in users to join become White and Black; everyone after that is a spectator. Moves are blocked until both seats are filled.
- ELO-style rating (starting at 1200), updated automatically when a game ends.
- Live move history (algebraic-style notation, matching the desktop client's format) and score, broadcast to all connected clients.
- A single-threaded queue per game serializes every state-changing operation (joins, moves, jumps, the periodic time tick) — the scheduled game-clock tick and incoming player commands can never race each other.
- Client-side sound (the server can't play sound on a remote browser's speakers — sound has to be triggered by the browser itself from the messages it receives).
- Fully responsive layout: the board and side panels resize to fit any window size without ever requiring the page itself to scroll.

**Not implemented yet**
- **Matchmaking** ("Play" button, automatic pairing by rating range) — planned, not built.
- **Rooms** (create/join by room code, spectators beyond the first two players) — planned, not built.
- Both of the above will require turning the current single global game into a per-room instance of the same `GameService`/`PlayerAssignmentService` pattern already in place.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 23 (source/target), built and run under a newer installed JDK |
| Build | Maven (multi-module reactor) |
| Desktop UI | Java Swing |
| Server | Spring Boot 4.1.0, STOMP over WebSocket (SockJS) |
| Persistence | SQLite (via Spring's `JdbcClient`), schema in `schema.sql` |
| Auth | BCrypt password hashing (`spring-security-crypto` only — not the full Spring Security framework) |
| Web client | Plain HTML/CSS/JS — no framework, no build step |
| Testing | JUnit 5, Mockito (server layer) |

## Running it

### Desktop
Run `com.chessgame.Main` (or the `App`/`GameWindow` entry point) directly from the `desktop` module.

### Server + web client
1. `mvn clean install` from the project root (a `clean` build is recommended any time modules have been restructured or files replaced from an archive).
2. Run `ServerApplication` (in `server`).
3. Open `http://localhost:8080/` in a browser — this is the login page. Register, then log in.
4. Open a second browser tab/window and log in as a different user to fill the second seat (White/Black).

## Testing

Each module has its own test suite (`mvn test` from the root runs all of them):
- `core` — the largest suite: rule engine, real-time arbiter/collision/cooldown behavior, the event bus, snapshot/DTO shape, and regression tests for the arrival-timing and premove-event fixes described above.
- `desktop` — UI-adjacent logic (controller, board mapping, move history formatting) and one end-to-end "a real click reaches the sound/log subscribers" test.
- `server` — service-layer unit tests (with Mockito) for auth, rating, player assignment, and the game service's ownership/readiness rules, plus a real (in-memory SQLite) integration test for the user repository.

## Project layout reference

```
core/src/main/java/com/chessgame/
├── model/        Board, Piece, Position, GameState
├── rules/        RuleEngine, per-piece move rules, MoveReason
├── realtime/     RealTimeArbiter, cooldowns, collisions, arrival resolution
├── engine/       GameEngine, MoveRequestHandler, MoveRecord, snapshots
├── bus/          SimpleEventBus + event types
├── logging/      Bus subscribers for console logging
└── io/           Board text-format parser/printer

server/src/main/java/com/chessgame/server/
├── controller/   STOMP + REST endpoints
├── service/      GameService, PlayerAssignmentService, AuthService, RatingService, ...
├── repository/   UserRepository (JdbcClient)
├── dto/          Wire-format messages
└── config/       DataSource configuration
```
