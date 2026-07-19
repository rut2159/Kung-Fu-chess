package com.chessgame.engine;
import com.chessgame.rules.MoveReason;
public final class MoveResult {
    private final boolean accepted;
    private final MoveReason reason;
    private MoveResult(boolean accepted, MoveReason reason) { this.accepted = accepted; this.reason = reason; }
    public static MoveResult accepted() { return new MoveResult(true, MoveReason.OK); }
    public static MoveResult rejected(MoveReason reason) { return new MoveResult(false, reason); }
    public static MoveResult premoveQueued() { return new MoveResult(false, MoveReason.PREMOVE_QUEUED); }
    public boolean isAccepted() { return accepted; }
    public MoveReason reason() { return reason; }
}
