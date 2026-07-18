package com.chessgame.ui.moves;

import com.chessgame.GameSession;
import com.chessgame.engine.GameEngine;
import com.chessgame.engine.GameListener;
import com.chessgame.engine.MoveRecord;
import com.chessgame.model.Piece;

import java.util.ArrayList;
import java.util.List;

public final class PlayerPanelController implements GameListener {

    private final Piece.Color color;
    private final MoveHistoryPanel panel;
    private int lastKnownMoveCount = -1;
    private int lastKnownScore = -1;

    public PlayerPanelController(GameSession session, Piece.Color color, String playerName) {
        this.color = color;
        this.panel = new MoveHistoryPanel(playerName);

        session.gameEngine.addListener(this);
        onGameStateChanged(session.gameEngine);
    }

    public MoveHistoryPanel panel() {
        return panel;
    }

    @Override
    public void onGameStateChanged(GameEngine engine) {
        List<MoveRecord> mine = new ArrayList<>();
        for (MoveRecord move : engine.moveHistory()) {
            if (move.color() == color) {
                mine.add(move);
            }
        }

        if (mine.size() != lastKnownMoveCount) {
            List<String[]> rows = new ArrayList<>();
            for (MoveRecord move : mine) {
                rows.add(new String[]{
                        MoveHistoryFormatter.formatTime(move.timestamp()),
                        MoveHistoryFormatter.formatMove(move)
                });
            }
            panel.setRows(rows);
            lastKnownMoveCount = mine.size();
        }

        int score = engine.score(color);
        if (score != lastKnownScore) {
            panel.setScore(score);
            lastKnownScore = score;
        }
    }
}
