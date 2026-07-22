package com.chessgame.server.service;

import org.junit.jupiter.api.Test;

import static com.chessgame.server.service.PlayerAssignmentService.Role.BLACK;
import static com.chessgame.server.service.PlayerAssignmentService.Role.VIEWER;
import static com.chessgame.server.service.PlayerAssignmentService.Role.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerAssignmentServiceTest {

    @Test
    void firstUserToJoin_becomesWhite() {
        PlayerAssignmentService service = new PlayerAssignmentService();

        PlayerAssignmentService.Role role = service.assign("session-1", "alice");

        assertEquals(WHITE, role);
    }

    @Test
    void secondDistinctUserToJoin_becomesBlack() {
        PlayerAssignmentService service = new PlayerAssignmentService();

        service.assign("session-1", "alice");
        PlayerAssignmentService.Role role = service.assign("session-2", "bob");

        assertEquals(BLACK, role);
    }

    @Test
    void thirdUserToJoin_becomesViewer() {
        PlayerAssignmentService service = new PlayerAssignmentService();

        service.assign("session-1", "alice");
        service.assign("session-2", "bob");
        PlayerAssignmentService.Role role = service.assign("session-3", "carol");

        assertEquals(VIEWER, role);
    }

    @Test
    void sameUsernameReconnectingWithNewSession_keepsItsOriginalRole() {
        PlayerAssignmentService service = new PlayerAssignmentService();
        service.assign("session-1", "alice");
        service.assign("session-2", "bob");

        // alice reconnects on a fresh WebSocket session (e.g. after a page refresh)
        PlayerAssignmentService.Role role = service.assign("session-3-new-connection", "alice");

        assertEquals(WHITE, role, "alice already owns WHITE - a reconnect must not bump her to VIEWER");
    }

    @Test
    void roleForSession_beforeAnyJoin_isViewer() {
        PlayerAssignmentService service = new PlayerAssignmentService();

        assertEquals(VIEWER, service.roleForSession("never-joined"));
    }

    @Test
    void roleForSession_afterAssign_matchesTheAssignedRole() {
        PlayerAssignmentService service = new PlayerAssignmentService();
        service.assign("session-1", "alice");

        assertEquals(WHITE, service.roleForSession("session-1"));
    }

    @Test
    void whiteAndBlackUsernames_areExposedAfterBothSeatsAreFilled() {
        PlayerAssignmentService service = new PlayerAssignmentService();
        service.assign("session-1", "alice");
        service.assign("session-2", "bob");

        assertEquals("alice", service.whiteUsername().orElseThrow());
        assertEquals("bob", service.blackUsername().orElseThrow());
    }
}
