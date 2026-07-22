package com.chessgame.server.service;

import com.chessgame.model.Piece;
import com.chessgame.server.repository.User;
import com.chessgame.server.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RatingService {

    private static final int K_FACTOR = 32;

    private final UserRepository userRepository;

    public RatingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Applies the standard ELO update to both players' ratings.
     * Does nothing if either seat wasn't filled by a registered user (e.g. a
     * lone player moving pieces around with no opponent yet, or a guest).
     */
    public void applyGameResult(String whiteUsername, String blackUsername, Piece.Color winner) {
        if (whiteUsername == null || blackUsername == null) {
            return;
        }
        Optional<User> whiteUser = userRepository.findByUsername(whiteUsername);
        Optional<User> blackUser = userRepository.findByUsername(blackUsername);
        if (whiteUser.isEmpty() || blackUser.isEmpty()) {
            return;
        }

        double whiteScore = (winner == Piece.Color.WHITE) ? 1.0 : (winner == Piece.Color.BLACK ? 0.0 : 0.5);
        double blackScore = 1.0 - whiteScore;

        int newWhiteRating = updatedElo(whiteUser.get().rating(), blackUser.get().rating(), whiteScore);
        int newBlackRating = updatedElo(blackUser.get().rating(), whiteUser.get().rating(), blackScore);

        userRepository.updateRating(whiteUsername, newWhiteRating);
        userRepository.updateRating(blackUsername, newBlackRating);
    }

    private int updatedElo(int playerRating, int opponentRating, double actualScore) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (opponentRating - playerRating) / 400.0));
        return (int) Math.round(playerRating + K_FACTOR * (actualScore - expectedScore));
    }
}
