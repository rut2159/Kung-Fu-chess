package com.chessgame.input;

import com.chessgame.engine.GameEngine;
import com.chessgame.engine.MoveResult;
import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

public final class Controller {
    private final Board board;
    private final BoardMapper boardMapper;
    private final GameEngine gameEngine;
    private Position selected;

    public Controller(Board board, BoardMapper boardMapper, GameEngine gameEngine) {
        this.board = board;
        this.boardMapper = boardMapper;
        this.gameEngine = gameEngine;
    }

    public ControllerResult click(int x, int y) {
        Position cell = boardMapper.pixelToCell(x, y);

        if (cell == null) {
            selected = null; // מבטל אם הייתה בחירה; לא-עושה-כלום אם לא הייתה - אותה שורה מכסה את שניהם
            return ControllerResult.noMove();
        }

        if (selected == null) {
            return handleFirstClick(cell);
        }

        return handleSecondClick(cell);
    }

    /** תוספת שלנו - קפיצה, לפי אותה תבנית כמו click. */
    public ControllerResult jump(int x, int y) {
        Position cell = boardMapper.pixelToCell(x, y);
        if (cell == null) {
            return ControllerResult.noMove();
        }

        if (cell.equals(selected)) {
            selected = null; // מבטלים בחירה אם קופצים על הכלי שכרגע נבחר
        }

        return ControllerResult.moveRequested(gameEngine.requestJump(cell));
    }

    private ControllerResult handleFirstClick(Position cell) {
        if (board.pieceAt(cell) == null) {
            return ControllerResult.noMove();
        }
        selected = cell;
        return ControllerResult.noMove();
    }

    private ControllerResult handleSecondClick(Position cell) {
        if (isFriendlyReselect(cell)) {
            selected = cell;
            return ControllerResult.noMove();
        }

        MoveResult result = gameEngine.requestMove(selected, cell);
        selected = null;
        return ControllerResult.moveRequested(result);
    }

    private boolean isFriendlyReselect(Position cell) {
        Piece clicked = board.pieceAt(cell);
        if (clicked == null) return false;
        Piece selectedPiece = board.pieceAt(selected);
        return selectedPiece != null && selectedPiece.isSameColorAs(clicked);
    }
}
