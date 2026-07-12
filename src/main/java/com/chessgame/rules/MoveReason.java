package com.chessgame.rules;

public enum MoveReason {
    OK("ok"),
    OUTSIDE_BOARD("outside_board"),
    EMPTY_SOURCE("empty_source"),
    FRIENDLY_DESTINATION("friendly_destination"),
    ILLEGAL_PIECE_MOVE("illegal_piece_move"),
    GAME_OVER("game_over"),
    MOTION_IN_PROGRESS("motion_in_progress");

    private final String label;

    MoveReason(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
