package com.chessgame.audio;

import com.chessgame.bus.events.GameOverEvent;
import com.chessgame.bus.events.MoveMadeEvent;
import com.chessgame.bus.events.MoveRejectedEvent;
import com.chessgame.engine.moves.MoveRecord;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.rules.MoveReason;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoundSubscriberTest {

    /** Records which classpath resources were "played", without touching real audio hardware. */
    private static final class FakeSoundPlayer implements SoundPlayer {
        private final List<String> played = new ArrayList<>();

        @Override
        public void play(String classpathResource) {
            played.add(classpathResource);
        }
    }

    @Test
    void onMoveMade_whenNotACapture_playsMoveSound() {
        FakeSoundPlayer fakePlayer = new FakeSoundPlayer();
        SoundSubscriber subscriber = new SoundSubscriber(fakePlayer);
        MoveRecord record = new MoveRecord(
                Piece.Color.WHITE, Piece.Kind.PAWN,
                new Position(6, 0), new Position(4, 0),
                false, 0L);

        subscriber.onMoveMade(new MoveMadeEvent(record));

        assertEquals(List.of("/sounds/move.wav"), fakePlayer.played);
    }

    @Test
    void onMoveMade_whenACapture_playsCaptureSound() {
        FakeSoundPlayer fakePlayer = new FakeSoundPlayer();
        SoundSubscriber subscriber = new SoundSubscriber(fakePlayer);
        MoveRecord record = new MoveRecord(
                Piece.Color.WHITE, Piece.Kind.PAWN,
                new Position(6, 4), new Position(5, 5),
                true, 0L);

        subscriber.onMoveMade(new MoveMadeEvent(record));

        assertEquals(List.of("/sounds/capture.wav"), fakePlayer.played);
    }

    @Test
    void onMoveRejected_playsIllegalMoveSound() {
        FakeSoundPlayer fakePlayer = new FakeSoundPlayer();
        SoundSubscriber subscriber = new SoundSubscriber(fakePlayer);

        subscriber.onMoveRejected(new MoveRejectedEvent(MoveReason.ILLEGAL_PIECE_MOVE));

        assertEquals(List.of("/sounds/illegal_move.wav"), fakePlayer.played);
    }

    @Test
    void onGameOver_playsGameOverSound() {
        FakeSoundPlayer fakePlayer = new FakeSoundPlayer();
        SoundSubscriber subscriber = new SoundSubscriber(fakePlayer);

        subscriber.onGameOver(new GameOverEvent());

        assertEquals(List.of("/sounds/game_over.wav"), fakePlayer.played);
    }
}
