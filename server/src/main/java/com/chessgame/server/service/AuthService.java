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

    private static final int MAX_USERNAME_LENGTH = 32;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserRepository userRepository) {
        this(userRepository, new BCryptPasswordEncoder());
    }

    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * שם משתמש עובר נרמול לפני *כל* גישה למסד. בלי זה, "רותי" ו-"רותי\n"
     * הם שני חשבונות נפרדים - וזה בדיוק מה שקרה: נרשם חשבון עם ירידת שורה
     * בסוף השם, שאי אפשר להתחבר אליו כי אף אחד לא יקליד אותה שוב.
     */
    private static String normalize(String username) {
        return username == null ? "" : username.strip();
    }

    public boolean register(String username, String password) {
        String name = normalize(username);
        if (name.isEmpty() || name.length() > MAX_USERNAME_LENGTH) {
            return false;
        }
        if (password == null || password.isEmpty()) {
            return false;
        }
        if (userRepository.findByUsername(name).isPresent()) {
            return false;
        }
        userRepository.insert(name, passwordEncoder.encode(password));
        return true;
    }

    public Optional<User> login(String username, String password) {
        if (password == null) {
            return Optional.empty();
        }
        return userRepository.findByUsername(normalize(username))
                .filter(user -> passwordEncoder.matches(password, user.passwordHash()));
    }
}
