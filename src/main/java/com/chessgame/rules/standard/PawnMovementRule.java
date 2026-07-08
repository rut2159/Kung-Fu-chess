package com.chessgame.rules.standard;

import com.chessgame.pieces.Piece;
import com.chessgame.rules.MoveContext;
import com.chessgame.rules.PieceMovementRule;

/**
 * PawnMovementRule / חוק תנועת חייל
 *
 * תפקיד: כיוון תלוי-צבע, צעד כפול מהשורה הראשונית, אכילה אלכסונית
 * בלבד, וקידום למלכה (resolveArrival). package-private (ר' הסבר
 * ב-KingMovementRule) - צריכה import ל-Piece כי מכריזה משתנים מסוג זה.
 */
class PawnMovementRule implements PieceMovementRule {
    @Override
    public boolean isValid(MoveContext ctx) {
        char color = ctx.movingPiece().getColor();
        int direction = (color == 'w') ? -1 : 1;
        int startRow = (color == 'w') ? ctx.board().getRowsCount() - 1 : 0;

        if (ctx.deltaCol() == 0 && ctx.toRow() == ctx.fromRow() + direction) {
            return ctx.board().getPiece(ctx.toRow(), ctx.toCol()).isEmpty();
        }

        if (ctx.deltaCol() == 0 && ctx.fromRow() == startRow && ctx.toRow() == ctx.fromRow() + 2 * direction) {
            Piece passedSquare = ctx.board().getPiece(ctx.fromRow() + direction, ctx.fromCol());
            Piece targetSquare = ctx.board().getPiece(ctx.toRow(), ctx.toCol());
            return passedSquare.isEmpty() && targetSquare.isEmpty();
        }

        if (ctx.deltaCol() == 1 && ctx.toRow() == ctx.fromRow() + direction) {
            Piece target = ctx.board().getPiece(ctx.toRow(), ctx.toCol());
            return !target.isEmpty() && target.isEnemyOf(ctx.movingPiece());
        }

        return false;
    }

    @Override
    public Piece resolveArrival(MoveContext ctx) {
        char color = ctx.movingPiece().getColor();
        int lastRow = (color == 'w') ? 0 : ctx.board().getRowsCount() - 1;
        if (ctx.toRow() == lastRow) {
            return Piece.of(color, 'Q');
        }
        return ctx.movingPiece();
    }
}
