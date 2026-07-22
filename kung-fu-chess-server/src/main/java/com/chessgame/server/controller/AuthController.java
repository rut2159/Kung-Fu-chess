package com.chessgame.server.controller;

import com.chessgame.server.dto.AuthRequest;
import com.chessgame.server.repository.User;
import com.chessgame.server.service.AuthService;
import com.chessgame.server.service.SessionTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final SessionTokenService sessionTokenService;

    public AuthController(AuthService authService, SessionTokenService sessionTokenService) {
        this.authService = authService;
        this.sessionTokenService = sessionTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody AuthRequest request) {
        boolean created = authService.register(request.username(), request.password());
        if (!created) {
            return ResponseEntity.status(409).body(Map.of("success", false, "message", "username already taken"));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthRequest request) {
        return authService.login(request.username(), request.password())
                .map(this::loginSuccessResponse)
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(Map.of("success", false, "message", "invalid username or password")));
    }

    private ResponseEntity<Map<String, Object>> loginSuccessResponse(User user) {
        // The token, not the username itself, is what the client will present
        // later when joining the game - see SessionTokenService's Javadoc for why.
        String token = sessionTokenService.issueToken(user.username());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "username", user.username(),
                "rating", user.rating(),
                "token", token
        ));
    }
}
