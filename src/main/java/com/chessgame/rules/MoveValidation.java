package com.chessgame.rules;

public final class MoveValidation {
    private final boolean valid;
    private final MoveReason reason;

    private MoveValidation(boolean valid, MoveReason reason) {
        this.valid = valid;
        this.reason = reason;
    }

    public static MoveValidation valid() {
        return new MoveValidation(true, MoveReason.OK);
    }

    public static MoveValidation invalid(MoveReason reason) {
        return new MoveValidation(false, reason);
    }

    public boolean isValid() {
        return valid;
    }

    public MoveReason reason() {
        return reason;
    }
}
