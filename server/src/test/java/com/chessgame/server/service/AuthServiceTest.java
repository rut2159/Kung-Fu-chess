package com.chessgame.server.service;

import com.chessgame.server.repository.User;
import com.chessgame.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void register_succeeds_whenUsernameIsFree() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        boolean created = authService.register("alice", "hunter2");

        assertTrue(created);
        verify(userRepository).insert(eq("alice"), any());
    }

    @Test
    void register_fails_whenUsernameAlreadyTaken() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User(1L, "alice", "hash", 1200)));

        boolean created = authService.register("alice", "hunter2");

        assertFalse(created);
        verify(userRepository, never()).insert(any(), any());
    }

    @Test
    void register_fails_whenTheUsernameLosesARaceAtTheDatabase() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        org.mockito.Mockito.doThrow(new DuplicateKeyException("username already exists"))
                .when(userRepository).insert(eq("alice"), any());

        boolean created = authService.register("alice", "hunter2");

        assertFalse(created, "a UNIQUE-constraint race must report 'taken', not throw");
    }

    @Test
    void register_fails_whenTheUsernameIsBlank() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);

        assertFalse(authService.register("   ", "hunter2"));
        verify(userRepository, never()).insert(any(), any());
    }

    @Test
    void register_fails_whenTheUsernameExceedsTheMaxLength() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);
        String tooLong = "a".repeat(33);

        assertFalse(authService.register(tooLong, "hunter2"));
        verify(userRepository, never()).insert(any(), any());
    }

    @Test
    void register_fails_whenThePasswordIsNull() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);

        assertFalse(authService.register("alice", null));
        verify(userRepository, never()).insert(any(), any());
    }

    @Test
    void register_fails_whenThePasswordIsEmpty() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);

        assertFalse(authService.register("alice", ""));
        verify(userRepository, never()).insert(any(), any());
    }

    @Test
    void register_normalizesTrailingWhitespaceInTheUsername() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        authService.register("alice\n", "hunter2");

        verify(userRepository).findByUsername("alice");
        verify(userRepository).insert(eq("alice"), any());
    }

    @Test
    void register_neverStoresThePlaintextPassword() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        authService.register("alice", "hunter2");

        org.mockito.ArgumentCaptor<String> storedHash = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(userRepository).insert(eq("alice"), storedHash.capture());
        assertFalse(storedHash.getValue().equals("hunter2"), "the raw password must never be stored as-is");
    }

    @Test
    void login_succeeds_whenPasswordMatchesTheStoredHash() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);
        String realHash = passwordEncoder.encode("hunter2");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User(1L, "alice", realHash, 1200)));

        Optional<User> result = authService.login("alice", "hunter2");

        assertTrue(result.isPresent());
    }

    @Test
    void login_fails_whenPasswordIsWrong() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);
        String realHash = passwordEncoder.encode("hunter2");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User(1L, "alice", realHash, 1200)));

        Optional<User> result = authService.login("alice", "wrong-password");

        assertTrue(result.isEmpty());
    }

    @Test
    void login_fails_whenUsernameDoesNotExist() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        Optional<User> result = authService.login("ghost", "anything");

        assertTrue(result.isEmpty());
    }

    @Test
    void login_fails_whenThePasswordIsNull() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);

        Optional<User> result = authService.login("alice", null);

        assertTrue(result.isEmpty());
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void login_normalizesTrailingWhitespaceInTheUsername() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);
        String realHash = passwordEncoder.encode("hunter2");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User(1L, "alice", realHash, 1200)));

        Optional<User> result = authService.login("alice\n", "hunter2");

        assertTrue(result.isPresent(), "login must normalize the username the same way register did");
    }
}
