package com.chessgame.server.controller;

import com.chessgame.server.dto.MatchmakingRequest;
import com.chessgame.server.dto.MatchmakingStatusResponse;
import com.chessgame.server.game.Matchmaker;
import com.chessgame.server.repository.User;
import com.chessgame.server.repository.UserRepository;
import com.chessgame.server.service.SessionTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {

    private static final int DEFAULT_RATING = 1200;

    private final SessionTokenService sessionTokenService;
    private final UserRepository userRepository;
    private final Matchmaker matchmaker;

    public MatchmakingController(SessionTokenService sessionTokenService, UserRepository userRepository,
                                  Matchmaker matchmaker) {
        this.sessionTokenService = sessionTokenService;
        this.userRepository = userRepository;
        this.matchmaker = matchmaker;
    }

    @PostMapping
    public ResponseEntity<MatchmakingStatusResponse> seek(@RequestBody MatchmakingRequest request) {
        Optional<String> username = sessionTokenService.resolveUsername(request.token());
        if (username.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        int rating = userRepository.findByUsername(username.get()).map(User::rating).orElse(DEFAULT_RATING);
        Matchmaker.Outcome outcome = matchmaker.poll(username.get(), rating);
        return ResponseEntity.ok(switch (outcome.status()) {
            case MATCHED -> MatchmakingStatusResponse.matched(outcome.roomId());
            case WAITING -> MatchmakingStatusResponse.waiting();
            case TIMED_OUT -> MatchmakingStatusResponse.timedOut();
        });
    }
}
