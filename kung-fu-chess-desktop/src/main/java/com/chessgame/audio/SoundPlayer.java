package com.chessgame.audio;

/**
 * Abstraction over "playing a sound clip", so that SoundSubscriber can be tested
 * with a fake implementation instead of touching real audio hardware.
 */
public interface SoundPlayer {
    void play(String classpathResource);
}
