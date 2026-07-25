package com.chessgame.server.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTokenServiceTest {

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
}
