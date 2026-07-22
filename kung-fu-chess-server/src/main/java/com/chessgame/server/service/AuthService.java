package com.chessgame.server.service;

import com.chessgame.server.repository.User;
import com.chessgame.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserRepository userRepository) {
        this(userRepository, new BCryptPasswordEncoder());
    }

    /** Package-visible constructor used by tests to inject a fake/predictable encoder if ever needed. */
    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Returns false if the username is already taken. */
    public boolean register(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            return false;
        }
        userRepository.insert(username, passwordEncoder.encode(password));
        return true;
    }

    /** Returns the user only if the username exists AND the password matches its hash. */
    public Optional<User> login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.passwordHash()));
    }
}
