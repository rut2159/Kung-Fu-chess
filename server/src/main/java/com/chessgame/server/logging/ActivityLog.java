package com.chessgame.server.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Single place every client/server activity line goes through.
 *
 * Routing all of it here rather than sprinkling log calls across controllers
 * means new message types are covered automatically, and the redaction below
 * cannot be forgotten at one of the call sites.
 */
public final class ActivityLog {

    private static final Logger log = LoggerFactory.getLogger("ACTIVITY");

    /**
     * Passwords are hashed with BCrypt before they reach the database, so the
     * database never holds one in clear text. A log file that recorded the
     * request body would hand them out anyway and defeat that entirely.
     *
     * Session tokens are redacted for the same reason: a leaked token is enough
     * to impersonate a player without knowing their password at all.
     *
     * The value part matches escaped characters too, so a password containing a
     * quote cannot leave a fragment of itself behind in the log.
     */
    private static final Pattern SECRET_FIELD = Pattern.compile(
            "(\"(?:password|token|passwordHash)\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"",
            Pattern.CASE_INSENSITIVE);

    /** Full game-state frames run to several kilobytes; the tail adds nothing to the log. */
    private static final int MAX_BODY_CHARS = 300;

    private ActivityLog() {
    }

    public static void inbound(String sessionId, String username, String destination, String body) {
        log.info("IN   session={} user={} dest={} body={}",
                nullSafe(sessionId), nullSafe(username), nullSafe(destination), summarise(body));
    }

    public static void outbound(String sessionId, String username, String destination, String body) {
        log.info("OUT  session={} user={} dest={} body={}",
                nullSafe(sessionId), nullSafe(username), nullSafe(destination), summarise(body));
    }

    public static void http(String method, String uri, int status, long millis) {
        log.info("HTTP {} {} -> {} ({} ms)", method, uri, status, millis);
    }

    public static void session(String event, String sessionId, String username) {
        log.info("SESS {} session={} user={}", event, nullSafe(sessionId), nullSafe(username));
    }

    public static void game(String message, Object... args) {
        log.info("GAME " + message, args);
    }

    static String summarise(String body) {
        if (body == null || body.isEmpty()) {
            return "-";
        }
        String redacted = redact(body);
        if (redacted.length() <= MAX_BODY_CHARS) {
            return redacted;
        }
        return redacted.substring(0, MAX_BODY_CHARS) + "...(" + redacted.length() + " chars)";
    }

    static String redact(String body) {
        return SECRET_FIELD.matcher(body).replaceAll("$1\"***\"");
    }

    private static String nullSafe(String value) {
        return value == null ? "-" : value;
    }
}
