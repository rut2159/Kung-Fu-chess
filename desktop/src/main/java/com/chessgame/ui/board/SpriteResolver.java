package com.chessgame.ui.board;

import com.chessgame.engine.snapshot.GameSnapshot;
import com.chessgame.model.Piece;


final class SpriteResolver {
    private static final String PIECES_BASE_PATH = "/pieces/";
    private static final int FRAME_COUNT = 5;

    String spritePath(GameSnapshot.PieceView piece) {
        String stateFolder = stateFolder(piece.state());
        if (stateFolder == null) {
            return null;
        }
        String code = pieceFolderCode(piece.color(), piece.kind());
        int frame = currentFrameIndex(piece.state());
        return PIECES_BASE_PATH + code + "/states/" + stateFolder + "/sprites/" + frame + ".png";
    }

    private String stateFolder(Piece.State state) {
        switch (state) {
            case IDLE: return "idle";
            case MOVING: return "move";
            case AIRBORNE: return "jump";
            case COOLDOWN_LONG: return "long_rest";
            case COOLDOWN_SHORT: return "short_rest";
            case CAPTURED: return null;
            default: return null;
        }
    }

    private String pieceFolderCode(Piece.Color color, Piece.Kind kind) {
        String kindLetter;
        switch (kind) {
            case KING: kindLetter = "K"; break;
            case QUEEN: kindLetter = "Q"; break;
            case ROOK: kindLetter = "R"; break;
            case BISHOP: kindLetter = "B"; break;
            case KNIGHT: kindLetter = "N"; break;
            case PAWN: kindLetter = "P"; break;
            default: throw new IllegalArgumentException("Unknown piece kind: " + kind);
        }
        String colorLetter = (color == Piece.Color.WHITE) ? "W" : "B";
        return kindLetter + colorLetter;
    }

    private int currentFrameIndex(Piece.State state) {
        if (state != Piece.State.MOVING && state != Piece.State.AIRBORNE) {
            return 1;
        }
        int fps = framesPerSecondFor(state);
        long elapsedFrames = (System.currentTimeMillis() * fps) / 1000;
        return (int) (elapsedFrames % FRAME_COUNT) + 1;
    }

    private int framesPerSecondFor(Piece.State state) {
        return state == Piece.State.AIRBORNE ? 8 : 12;
    }
}
