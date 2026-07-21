package com.chessgame.audio;

import com.chessgame.bus.events.GameOverEvent;
import com.chessgame.bus.events.MoveMadeEvent;
import com.chessgame.bus.events.MoveRejectedEvent;

public final class SoundSubscriber {

    private final SoundPlayer soundPlayer;

    public SoundSubscriber() {
        this(new ClipPlayer());
    }

    public SoundSubscriber(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }

    public void onMoveMade(MoveMadeEvent event) {
        if (event.record().isCapture()) {
            soundPlayer.play("/sounds/capture.wav");
        } else {
            soundPlayer.play("/sounds/move.wav");
        }
    }

    public void onMoveRejected(MoveRejectedEvent event) {
        soundPlayer.play("/sounds/illegal_move.wav");
    }

    public void onGameOver(GameOverEvent event) {
        soundPlayer.play("/sounds/game_over.wav");
    }
}
