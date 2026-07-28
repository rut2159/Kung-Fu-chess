# Error-handling and test-coverage pass — 2026-07-28

## Goal

Before continuing with any new feature work, do a hardening pass on the existing
codebase: find real gaps in exception handling, and raise test coverage on
whatever currently has none — without adding defensive code or tests for
scenarios that can't happen.

## Methodology

`mvn test` runs under JDK 26 (see `java.release` in the root `pom.xml`), and
JaCoCo 0.8.15 — the newest release available on Maven Central at the time of
this pass — does not yet instrument class file version 70. It silently
produces no `.exec` file, so no numeric coverage report was available.

In its place, this was a manual, two-part audit:

1. **Every real I/O/network/JDBC boundary** in `core`, `desktop`, and `server`
   was read and checked for unhandled exceptions.
2. **Every class with non-trivial logic and zero existing test file** was
   identified and given tests, unless it was pure Spring configuration, an
   entry point (`Main`, `ServerApplication`, `GuiApp`), or a plain DTO/record
   — writing tests for those would be coverage theater, not verification.

Classes already exercised only *indirectly* (e.g. `BishopRule`, `CollisionManager`
via `PieceRulesTest` / `CollisionAndCooldownTest`) were left alone — that's the
established testing style in this codebase (test observable behavior through
the public entry point, not every private collaborator), and it was respected
rather than overridden.

## Fixes

### `server/.../service/AuthService.java` — registration race condition
`register()` did a `findByUsername` check followed by `insert()`, with no
transaction or lock between them. Two concurrent registrations for the same
username could both pass the check, and the losing `insert()` would throw an
unhandled `DuplicateKeyException` (from the `users.username` UNIQUE
constraint) — surfacing as a raw HTTP 500 instead of "username already taken".

**Fix:** wrap `insert()` in `try { ... } catch (DuplicateKeyException) { return false; }`.
The database's UNIQUE constraint is the real source of truth; the pre-check is
just a fast path for the common case.

### `server/.../session/SessionLifecycleListener.java` — inconsistent defensive style
Every other side-effecting boundary in this server already followed the same
rule: *a failing side effect must never break the framework's own path*
(`StompActivityInterceptor.preSend` swallows exceptions so a bad log line
can't block a STOMP message; `HttpActivityFilter` swallows logging failures so
they can't fail an HTTP response). `onDisconnect` was the one exception —
`roomRegistry.leave(sessionId)` ran unguarded, and Spring propagates
`@EventListener` exceptions back into its own WebSocket teardown path.

**Fix:** wrapped the call in `try/catch (RuntimeException)`, logging the
failure instead of letting it disrupt socket teardown or other listeners on
the same event — brought in line with the rest of the codebase's own rule.

### `server/.../service/SessionTokenService.java` — untestable TTL (refactor, not a bug)
Token expiry used `Instant.now()` directly, so the 12-hour TTL branch could
only ever be exercised by waiting 12 real hours. This mirrors a pattern
already used elsewhere in the codebase (`AuthService` takes an injectable
`PasswordEncoder` via a package-private constructor purely for tests).

**Fix:** added an injectable `java.time.Clock` — a public no-arg constructor
(`Clock.systemUTC()`, used by Spring) and a package-private
`SessionTokenService(Clock)` constructor for tests. No behavior change for
production; the TTL logic is now verifiable with a controllable clock instead
of a real wait.

## Checked and found already correct (no change made)

| File | Why it's fine as-is |
|---|---|
| `core/.../io/BoardParser.java` | Its one real boundary (raw text → domain model) already validates thoroughly and throws a typed `BoardParseException` with clear error codes. Wrapping it in another try/catch would just re-catch its own exception. |
| `desktop/.../ui/board/ImageLoader.java` | Try-with-resources around the classpath stream; `IOException` already caught and rethrown with context. |
| `desktop/.../audio/ClipPlayer.java` | Comment-documented on purpose: playback failures (missing file, no audio hardware) are deliberately logged and swallowed so a missing sound effect can never crash the game. |
| `server/.../config/StaticResourceConfig.java` | Uses non-throwing `Files.isDirectory`/`isRegularFile` checks; the "not found" path already logs a loud warning and falls back cleanly. |
| `server/.../config/DataSourceConfig.java` | No I/O happens at bean-construction time (the JDBC connection opens lazily on first use). |
| `server/.../controller/AssetController.java` | Already checks `resource.exists()` and returns 404 cleanly before serving. |
| `server/.../logging/HttpActivityFilter.java`, `StompActivityInterceptor.java` | Already wrap their logging call in `try/catch (RuntimeException)` with a comment explaining why a log failure must never affect the real request/message. |
| `server/.../game/GameRoom.java` (`submit()`) | Already correctly unwraps `ExecutionException` to preserve the real cause and restores the interrupt flag on `InterruptedException`. |

## New tests

| Test class | What it covers | Why it had none before |
|---|---|---|
| `AuthServiceTest` (+8) | The race-condition fix; blank/too-long username; null/empty password; a regression test for the exact "trailing-newline username" bug described (but never tested) in the class's own comment; the same normalization on `login`. | Existing tests only covered the non-racing, valid-input paths. |
| `SessionTokenServiceTest` (+5) | Null token; expiry exactly at/just past the 12h boundary (via the new injectable clock); `revoke()`, including with a null token. | TTL expiry and `revoke()` were literally untestable before the clock refactor. |
| `SessionLifecycleListenerTest` (new) | Disconnect releases the room; an exception from `RoomRegistry.leave` is swallowed, not propagated; `onConnected` touches nothing. | No test file existed at all. |
| `GameTickerTest` (new) | The single `@Scheduled` tick calls both `roomRegistry.tickAll` and `matchmaker.tick` with the same interval. | This is the only wiring point connecting the scheduler to both subsystems; a typo here would silently stop one of them. No test existed. |
| `StompActivityInterceptorTest` (new) | Username resolution from the session, inbound vs. outbound log direction, the "no session" case, and — the most valuable case — that a failing `RoomRegistry` lookup never blocks the message from being sent. Uses a Logback `ListAppender` attached to the `"ACTIVITY"` logger to assert on actual log content, not just "no exception". | No test file existed. |
| `ActivityLogTest` (new) | Password/token/passwordHash redaction (case-insensitive), that ordinary fields are left alone, truncation of long bodies, and — importantly — that redaction happens *before* truncation, so a long secret can't leak a partial value. | This is the logic that keeps secrets out of the log files; it had zero coverage despite being security-relevant. |

## Explicitly not touched, and why

- **Spring `@Configuration` classes** (`WebSocketConfig`, etc.), **entry points**
  (`Main`, `ServerApplication`, `GuiApp`), and **plain DTO/records** — no
  behavior to unit-test; these are proven by the app booting and by the
  higher-level tests that already exercise them.
- **`core`'s per-piece/collision/motion classes** (`BishopRule`,
  `CollisionManager`, `MotionManager`, etc.) — already exercised through
  `PieceRulesTest`, `RuleEngineTest`, `CollisionAndCooldownTest`, and friends.
  Adding a 1:1 test file per class would duplicate what's already verified and
  break from this codebase's established "test through the public engine
  entry point" style.

## Verification

- `mvn test` from the repo root: **344 tests, 0 failures, 0 errors**
  (132 `core` + 72 `desktop` + 140 `server` — up from 296 at the start of this
  pass; `server` alone went from 110 to 140).
- Confirmed the one-off ~37-minute single-test-class timing seen mid-session
  was a transient environment hiccup (coincided with an unrelated tool
  connection drop), not a real hang — re-ran the affected classes in isolation
  afterward and they completed in under 6 seconds each.
