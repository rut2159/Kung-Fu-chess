package com.chessgame.server.controller;

import com.chessgame.server.dto.MatchmakingRequest;
import com.chessgame.server.dto.MatchmakingStatusResponse;
import com.chessgame.server.game.Matchmaker;
import com.chessgame.server.repository.User;
import com.chessgame.server.repository.UserRepository;
import com.chessgame.server.service.SessionTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchmakingControllerTest {

    @Mock
    private SessionTokenService sessionTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Matchmaker matchmaker;

    private MatchmakingController newController() {
        return new MatchmakingController(sessionTokenService, userRepository, matchmaker);
    }

    @Test
    void seek_returns401_whenTheTokenIsUnknown() {
        MatchmakingController controller = newController();
        when(sessionTokenService.resolveUsername("bad-token")).thenReturn(Optional.empty());

        ResponseEntity<MatchmakingStatusResponse> response = controller.seek(new MatchmakingRequest("bad-token"));

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void seek_looksUpTheCallersRatingBeforePolling() {
        MatchmakingController controller = newController();
        when(sessionTokenService.resolveUsername("good-token")).thenReturn(Optional.of("alice"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User(1L, "alice", "hash", 1350)));
        when(matchmaker.poll("alice", 1350)).thenReturn(new Matchmaker.Outcome(Matchmaker.Status.WAITING, null));

        ResponseEntity<MatchmakingStatusResponse> response = controller.seek(new MatchmakingRequest("good-token"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("WAITING", response.getBody().status());
    }

    @Test
    void seek_fallsBackToTheDefaultRating_whenTheUserRowIsMissing() {
        MatchmakingController controller = newController();
        when(sessionTokenService.resolveUsername("good-token")).thenReturn(Optional.of("alice"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(matchmaker.poll(eq("alice"), eq(1200)))
                .thenReturn(new Matchmaker.Outcome(Matchmaker.Status.WAITING, null));

        ResponseEntity<MatchmakingStatusResponse> response = controller.seek(new MatchmakingRequest("good-token"));

        assertEquals("WAITING", response.getBody().status());
    }

    @Test
    void seek_returnsTheRoomId_whenMatched() {
        MatchmakingController controller = newController();
        when(sessionTokenService.resolveUsername("good-token")).thenReturn(Optional.of("alice"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User(1L, "alice", "hash", 1200)));
        when(matchmaker.poll("alice", 1200))
                .thenReturn(new Matchmaker.Outcome(Matchmaker.Status.MATCHED, "K7F2QX"));

        ResponseEntity<MatchmakingStatusResponse> response = controller.seek(new MatchmakingRequest("good-token"));

        assertEquals("MATCHED", response.getBody().status());
        assertEquals("K7F2QX", response.getBody().roomId());
    }

    @Test
    void seek_reportsATimeout() {
        MatchmakingController controller = newController();
        when(sessionTokenService.resolveUsername("good-token")).thenReturn(Optional.of("alice"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User(1L, "alice", "hash", 1200)));
        when(matchmaker.poll("alice", 1200))
                .thenReturn(new Matchmaker.Outcome(Matchmaker.Status.TIMED_OUT, null));

        ResponseEntity<MatchmakingStatusResponse> response = controller.seek(new MatchmakingRequest("good-token"));

        assertEquals("TIMED_OUT", response.getBody().status());
    }
}
