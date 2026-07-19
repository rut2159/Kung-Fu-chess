package com.chessgame.engine;

import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.model.Position;
import com.chessgame.rules.MoveReason;
import com.chessgame.rules.MoveValidation;
import com.chessgame.rules.RuleEngine;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.model.Piece;

import java.util.ArrayList;
import java.util.List;

/**
 * מתאם את המהלכים: מקבל בקשות (תנועה/קפיצה/premove), בודק תקינות מול
 * ה-RuleEngine וה-RealTimeArbiter, ומודיע ל-listeners כשמשהו משתנה.
 * לא יודע איך בונים snapshot לתצוגה (זה תפקידה של GameSnapshotFactory).
 */
public final class GameEngine {
    private final Board board;
    private final GameState gameState;
    private final RuleEngine ruleEngine;
    private final RealTimeArbiter realTimeArbiter;
    private final MoveHistory moveHistory = new MoveHistory();
    private final PremoveManager premoveManager = new PremoveManager();
    private final GameSnapshotFactory snapshotFactory;
    private final List<Piece> roster;
    private final List<GameListener> listeners = new ArrayList<>();

    public GameEngine(Board board, GameState gameState, RuleEngine ruleEngine, RealTimeArbiter realTimeArbiter) {
        this.board = board;
        this.gameState = gameState;
        this.ruleEngine = ruleEngine;
        this.realTimeArbiter = realTimeArbiter;
        this.roster = board.allPieces();
        this.snapshotFactory = new GameSnapshotFactory(board, realTimeArbiter, premoveManager, roster);
    }

    public void addListener(GameListener listener) {
        listeners.add(listener);
    }

    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (GameListener listener : listeners) {
            listener.onGameStateChanged(this);
        }
    }

    public int score(Piece.Color color) {
        return ScoreCalculator.score(roster, color);
    }

    public MoveResult requestMove(Position source, Position destination) {
        if (gameState.isGameOver()) {
            return MoveResult.rejected(MoveReason.GAME_OVER);
        }

        // הכלי בקירור (לא "בתנועה" ולא "באוויר" - אלה מטופלים ע"י canStartMotion
        // למטה) - זה בדיוק המצב שבו מותר לתזמן premove במקום סתם לדחות.
        if (realTimeArbiter.isPieceCoolingDown(source)) {
            return handlePremoveRequest(source, destination);
        }

        if (!realTimeArbiter.canStartMotion(source, destination)) {
            return MoveResult.rejected(MoveReason.MOTION_IN_PROGRESS);
        }

        MoveValidation legality = ruleEngine.validateMove(source, destination);
        if (!legality.isValid()) {
            return MoveResult.rejected(legality.reason());
        }

        return executeMove(source, destination);
    }

    /**
     * הכלי בקירור: אם המהלך המבוקש חוקי - שומרים אותו כ"כוונה עתידית"
     * (דורס כל premove קודם לאותו כלי). אם הוא לא חוקי - זו בדיוק הדרך
     * ל"בטל" premove קיים (מהלך-לא-חוקי מוחק את השמירה, בלי לבצע כלום).
     */
    private MoveResult handlePremoveRequest(Position source, Position destination) {
        MoveValidation legality = ruleEngine.validateMove(source, destination);
        if (!legality.isValid()) {
            premoveManager.clear(source);
            return MoveResult.rejected(legality.reason());
        }

        premoveManager.set(source, destination);
        return MoveResult.premoveQueued();
    }

    private MoveResult executeMove(Position source, Position destination) {
        Piece piece = board.pieceAt(source);
        boolean capture = board.pieceAt(destination) != null;
        long timestamp = realTimeArbiter.gameClock();

        realTimeArbiter.startMotion(source, destination);
        moveHistory.record(new MoveRecord(piece.color(), piece.kind(), source, destination, capture, timestamp));
        notifyListeners();
        return MoveResult.accepted();
    }

    public List<MoveRecord> moveHistory() {
        return moveHistory.all();
    }

    public MoveResult requestJump(Position position) {
        if (gameState.isGameOver()) {
            return MoveResult.rejected(MoveReason.GAME_OVER);
        }

        if (!realTimeArbiter.canStartJump(position)) {
            return MoveResult.rejected(MoveReason.MOTION_IN_PROGRESS);
        }

        if (board.pieceAt(position) == null) {
            return MoveResult.rejected(MoveReason.EMPTY_SOURCE);
        }

        realTimeArbiter.startJump(position);
        return MoveResult.accepted();
    }

    public void wait(int milliseconds) {
        boolean kingCaptured = realTimeArbiter.advanceTime(milliseconds);
        if (kingCaptured) {
            gameState.setGameOver(true);
        }

        for (Position position : realTimeArbiter.justExpiredCooldownPositions()) {
            firePremoveIfAny(position);
        }

        notifyListeners();
    }

    /** אם יש כוונה שמורה עבור הכלי הזה - מנקים אותה ומריצים אותה עכשיו דרך requestMove הרגיל. */
    private void firePremoveIfAny(Position source) {
        premoveManager.get(source).ifPresent(destination -> {
            premoveManager.clear(source);
            requestMove(source, destination);
        });
    }

    public GameSnapshot snapshot(Position selectedCell) {
        return snapshotFactory.build(selectedCell, gameState.isGameOver());
    }
}
