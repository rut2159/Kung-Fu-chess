package com.chessgame.server.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityLogTest {

    @Test
    void redact_hidesAPasswordField() {
        String body = "{\"username\":\"alice\",\"password\":\"hunter2\"}";

        String redacted = ActivityLog.redact(body);

        assertFalse(redacted.contains("hunter2"), "the raw password must never survive redaction");
        assertTrue(redacted.contains("\"password\":\"***\""));
        assertTrue(redacted.contains("alice"), "non-secret fields must be left alone");
    }

    @Test
    void redact_hidesATokenField() {
        String body = "{\"token\":\"782cfbc9-abc2-4ed7-8fbe-0ae5baa7fb59\"}";

        String redacted = ActivityLog.redact(body);

        assertFalse(redacted.contains("782cfbc9"));
        assertTrue(redacted.contains("\"token\":\"***\""));
    }

    @Test
    void redact_hidesAPasswordHashField() {
        String body = "{\"passwordHash\":\"$2a$10$abcdefg\"}";

        String redacted = ActivityLog.redact(body);

        assertFalse(redacted.contains("$2a$10$abcdefg"));
    }

    @Test
    void redact_isCaseInsensitive() {
        String body = "{\"PASSWORD\":\"hunter2\"}";

        String redacted = ActivityLog.redact(body);

        assertFalse(redacted.contains("hunter2"));
    }

    @Test
    void redact_leavesOrdinaryBodiesUntouched() {
        String body = "{\"fromRow\":6,\"fromCol\":4,\"toRow\":4,\"toCol\":4}";

        assertEquals(body, ActivityLog.redact(body));
    }

    @Test
    void summarise_returnsAPlaceholder_forNullOrEmpty() {
        assertEquals("-", ActivityLog.summarise(null));
        assertEquals("-", ActivityLog.summarise(""));
    }

    @Test
    void summarise_truncatesLongBodiesAndReportsTheOriginalLength() {
        String longBody = "x".repeat(500);

        String result = ActivityLog.summarise(longBody);

        assertTrue(result.startsWith("x".repeat(300)));
        assertTrue(result.endsWith("...(500 chars)"));
    }

    @Test
    void summarise_redactsBeforeTruncating() {
        String body = "{\"password\":\"" + "x".repeat(500) + "\"}";

        String result = ActivityLog.summarise(body);

        assertFalse(result.contains("xxxx"), "a long secret must be redacted, not leaked through truncation");
    }
}
