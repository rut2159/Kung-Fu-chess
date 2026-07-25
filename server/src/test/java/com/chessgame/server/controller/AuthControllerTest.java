package com.chessgame.server.controller;

import com.chessgame.server.dto.AuthRequest;
import com.chessgame.server.repository.User;
import com.chessgame.server.service.AuthService;
import com.chessgame.server.service.SessionTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private SessionTokenService sessionTokenService;

    private AuthController newController() {
        return new AuthController(authService, sessionTokenService);
    }

    @Test
    void register_returns200_whenServiceAcceptsIt() {
        AuthController controller = newController();
        when(authService.register("alice", "hunter2")).thenReturn(true);

        ResponseEntity<?> response = controller.register(new AuthRequest("alice", "hunter2"));

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void register_returns409_whenUsernameAlreadyTaken() {
        AuthController controller = newController();
        when(authService.register("alice", "hunter2")).thenReturn(false);

        ResponseEntity<?> response = controller.register(new AuthRequest("alice", "hunter2"));

        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void login_returns200AndAToken_whenCredentialsAreValid() {
        AuthController controller = newController();
        when(authService.login("alice", "hunter2"))
                .thenReturn(Optional.of(new User(1L, "alice", "hash", 1200)));
        when(sessionTokenService.issueToken("alice")).thenReturn("tok-abc");

        ResponseEntity<?> response = controller.login(new AuthRequest("alice", "hunter2"));

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void login_returns401_whenCredentialsAreInvalid() {
        AuthController controller = newController();
        when(authService.login("alice", "wrong")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.login(new AuthRequest("alice", "wrong"));

        assertEquals(401, response.getStatusCode().value());
    }
}
