package com.chessgame.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Plays a single WAV resource from the classpath. Knows nothing about chess,
 * moves, or events - only "how" to play a sound file.
 *
 * Playback failures (no audio device available, missing file, unsupported
 * format) are logged and swallowed rather than thrown: a missing sound
 * should never crash the game.
 */
public final class ClipPlayer implements SoundPlayer {

    /**
     * Plays the WAV file at the given classpath location, e.g. "/sounds/move.wav".
     * Returns immediately; playback happens asynchronously.
     */
    @Override
    public void play(String classpathResource) {
        try (InputStream rawStream = ClipPlayer.class.getResourceAsStream(classpathResource)) {
            if (rawStream == null) {
                System.err.println("[SOUND] Resource not found: " + classpathResource);
                return;
            }

            try (AudioInputStream audioStream =
                         AudioSystem.getAudioInputStream(new BufferedInputStream(rawStream))) {
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
            }
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException
                 | IllegalArgumentException e) {
            // IllegalArgumentException covers "no matching audio line" - e.g. no sound
            // hardware available at all (common in containers/headless environments).
            System.err.println("[SOUND] Failed to play " + classpathResource + ": " + e.getMessage());
        }
    }
}
