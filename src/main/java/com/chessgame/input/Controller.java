package com.chessgame.input;

import com.chessgame.engine.GameEngine;
import com.chessgame.engine.GameSnapshot;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.Optional;

public final class Controller {
    private final BoardMapper boardMapper;
    private final GameEngine gameEngine;
    private Position selected;

    public Controller(BoardMapper boardMapper, GameEngine gameEngine) {
        this.boardMapper = boardMapper;
        this.gameEngine = gameEngine;
    }

    public Position selectedCell() { return selected; }

    public void setCellSizePx(int cellSizePx) {
        boardMapper.setCellSizePx(cellSizePx);
    }

    public ControllerResult click(int x, int y) {
        Position cell = boardMapper.pixelToCell(x, y);

        if (cell == null) {
            selected = null;
            return ControllerResult.noMove();
        }

        if (selected == null) {
            return handleFirstClick(cell);
        }

        return handleSecondClick(cell);
    }

    public ControllerResult jump(int x, int y) {
        Position cell = boardMapper.pixelToCell(x, y);
        if (cell == null) {
            return ControllerResult.noMove();
        }

        if (cell.equals(selected)) {
            selected = null;
        }

        return ControllerResult.moveRequested(gameEngine.requestJump(cell));
    }

    private ControllerResult handleFirstClick(Position cell) {
        if (pieceColorAt(cell).isEmpty()) {
            return ControllerResult.noMove();
        }
        selected = cell;
        return ControllerResult.noMove();
    }

    private ControllerResult handleSecondClick(Position cell) {
        Position from = selected;
        selected = null;

        if (cell.equals(from)) {
            return ControllerResult.moveRequested(gameEngine.requestJump(cell));
        }

        return ControllerResult.moveRequested(gameEngine.requestMove(from, cell));
    }

    private Optional<Piece.Color> pieceColorAt(Position cell) {
        GameSnapshot snapshot = gameEngine.snapshot(selected);
        for (GameSnapshot.PieceView piece : snapshot.pieces()) {
            if (piece.position().equals(cell)) {
                return Optional.of(piece.color());
            }
        }
        return Optional.empty();
    }
}
