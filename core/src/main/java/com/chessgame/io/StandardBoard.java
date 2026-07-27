package com.chessgame.io;

import com.chessgame.model.Board;

public final class StandardBoard {

    public static final String SETUP_TEXT = """
            bR bN bB bQ bK bB bN bR
            bP bP bP bP bP bP bP bP
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            wP wP wP wP wP wP wP wP
            wR wN wB wQ wK wB wN wR""";

    private StandardBoard() {
    }

    public static Board create() {
        return new BoardParser().parse(SETUP_TEXT);
    }
}
