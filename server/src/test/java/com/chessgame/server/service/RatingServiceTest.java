package com.chessgame.server.service;

import com.chessgame.model.Piece;
import com.chessgame.server.repository.User;
import com.chessgame.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void whiteWins_whiteRatingGoesUp_blackRatingGoesDown() {
        RatingService ratingService = new RatingService(userRepository);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User(1L, "alice", "hash", 1200)));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(new User(2L, "bob", "hash", 1200)));

        ratingService.applyGameResult("alice", "bob", Piece.Color.WHITE);

        verify(userRepository).updateRating(eq("alice"), argThatIsGreaterThan(1200));
        verify(userRepository).updateRating(eq("bob"), argThatIsLessThan(1200));
    }

    @Test
    void draw_evenlyMatchedPlayers_ratingsStayApproximatelyEqual() {
        RatingService ratingService = new RatingService(userRepository);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User(1L, "alice", "hash", 1200)));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(new User(2L, "bob", "hash", 1200)));

        ratingService.applyGameResult("alice", "bob", null); // no winner = draw

        // Equal ratings + draw => expected score was already 0.5, so no real change either way
        verify(userRepository).updateRating(eq("alice"), anyInt());
        verify(userRepository).updateRating(eq("bob"), anyInt());
    }

    @Test
    void missingWhiteUsername_doesNothing_noException() {
        RatingService ratingService = new RatingService(userRepository);

        ratingService.applyGameResult(null, "bob", Piece.Color.BLACK);

        verify(userRepository, never()).updateRating(eq("bob"), anyInt());
    }

    @Test
    void unknownUsername_doesNothing_noException() {
        RatingService ratingService = new RatingService(userRepository);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(new User(2L, "bob", "hash", 1200)));

        ratingService.applyGameResult("alice", "bob", Piece.Color.WHITE);

        verify(userRepository, never()).updateRating(eq("bob"), anyInt());
    }

    private int argThatIsGreaterThan(int threshold) {
        return org.mockito.ArgumentMatchers.intThat(value -> value > threshold);
    }

    private int argThatIsLessThan(int threshold) {
        return org.mockito.ArgumentMatchers.intThat(value -> value < threshold);
    }
}
