package com.chessgame.server.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTokenServiceTest {

    /** A Clock a test can move forward by hand, so a 12h TTL doesn't need a real 12h wait. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void issuedToken_resolvesBackToTheSameUsername() {
        SessionTokenService service = new SessionTokenService();

        String token = service.issueToken("alice");

        assertEquals("alice", service.resolveUsername(token).orElseThrow());
    }

    @Test
    void unknownToken_resolvesToNothing() {
        SessionTokenService service = new SessionTokenService();

        assertTrue(service.resolveUsername("never-issued").isEmpty());
    }

    @Test
    void twoIssuedTokens_areDifferent_evenForTheSameUsername() {
        SessionTokenService service = new SessionTokenService();

        String first = service.issueToken("alice");
        String second = service.issueToken("alice");

        assertTrue(!first.equals(second), "each login should get its own unguessable token");
    }

    @Test
    void resolveUsername_returnsNothing_forANullToken() {
        SessionTokenService service = new SessionTokenService();

        assertTrue(service.resolveUsername(null).isEmpty());
    }

    @Test
    void aTokenExpiresOnceItsTwelveHourTtlHasPassed() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        SessionTokenService service = new SessionTokenService(clock);
        String token = service.issueToken("alice");

        clock.advance(Duration.ofHours(12).plusSeconds(1));

        assertTrue(service.resolveUsername(token).isEmpty(), "a token must not outlive its 12h TTL");
    }

    @Test
    void aTokenIsStillValidOneSecondBeforeItExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        SessionTokenService service = new SessionTokenService(clock);
        String token = service.issueToken("alice");

        clock.advance(Duration.ofHours(12).minusSeconds(1));

        assertEquals("alice", service.resolveUsername(token).orElseThrow());
    }

    @Test
    void revoke_invalidatesTheToken() {
        SessionTokenService service = new SessionTokenService();
        String token = service.issueToken("alice");

        service.revoke(token);

        assertTrue(service.resolveUsername(token).isEmpty());
    }

    @Test
    void revoke_toleratesANullToken() {
        SessionTokenService service = new SessionTokenService();

        service.revoke(null);
    }
}
