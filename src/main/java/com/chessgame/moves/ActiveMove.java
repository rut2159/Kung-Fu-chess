package com.chessgame.moves;

import com.chessgame.pieces.Piece;

/**
 * ActiveMove / מהלך פעיל
 *
 * תפקיד: מייצג כלי שזז כרגע. public כי GameEngine (חבילת השורש) יוצר
 * מופעים חדשים ישירות (new ActiveMove(...)). שימו לב: השדות עצמם
 * נשארים package-private (בלי public) - הם נגישים רק בתוך חבילת moves
 * (על ידי MoveManager ו-MoveArrivalProcessor). GameEngine, שנמצא
 * בחבילה אחרת, יכול ליצור ActiveMove אבל לא לקרוא/לשנות את השדות שלו
 * ישירות - זו בדיוק העצמת encapsulation שחבילות אמיתיות מאפשרות.
 */
public final class ActiveMove {
    final int fromRow, fromCol;
    final int toRow, toCol;
    final Piece piece;
    final long arrivalTime;

    public ActiveMove(int fromRow, int fromCol, int toRow, int toCol, Piece piece, long arrivalTime) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.piece = piece;
        this.arrivalTime = arrivalTime;
    }

    /** האם המהלך כבר הגיע ליעדו, בהינתן זמן המשחק הנוכחי. */
    boolean hasArrived(long gameClock) {
        return gameClock >= arrivalTime;
    }
}
