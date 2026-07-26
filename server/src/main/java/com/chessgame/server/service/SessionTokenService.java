package com.chessgame.server.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * טוקנים לזיהוי מי באמת עומד מאחורי חיבור ה-STOMP.
 *
 * הגרסה הקודמת החזיקה מפה שגדלה לנצח: טוקן שהונפק פעם אחת נשאר תקף עד
 * שהשרת נופל. זה גם דליפת זיכרון איטית (כל התחברות מוסיפה רשומה שלא
 * נמחקת אף פעם) וגם בעיית אבטחה - טוקן שדלף, למשל מה-URL של דף המשחק,
 * לא פג לעולם. עכשיו לכל טוקן יש תפוגה, ורשומות שפג תוקפן מפונות.
 */
@Service
public class SessionTokenService {

    private static final Duration TOKEN_TTL = Duration.ofHours(12);

    private record Entry(String username, Instant expiresAt) {
        boolean isExpired(Instant now) {
            return now.isAfter(expiresAt);
        }
    }

    private final Map<String, Entry> entriesByToken = new ConcurrentHashMap<>();

    public String issueToken(String username) {
        purgeExpired();
        String token = UUID.randomUUID().toString();
        entriesByToken.put(token, new Entry(username, Instant.now().plus(TOKEN_TTL)));
        return token;
    }

    public Optional<String> resolveUsername(String token) {
        if (token == null) {
            return Optional.empty();
        }
        Entry entry = entriesByToken.get(token);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired(Instant.now())) {
            entriesByToken.remove(token);
            return Optional.empty();
        }
        return Optional.of(entry.username());
    }

    /** ביטול מפורש - לכפתור התנתקות, וכל מקום שבו טוקן צריך להפסיק להיות תקף מיד. */
    public void revoke(String token) {
        if (token != null) {
            entriesByToken.remove(token);
        }
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        entriesByToken.values().removeIf(entry -> entry.isExpired(now));
    }
}
