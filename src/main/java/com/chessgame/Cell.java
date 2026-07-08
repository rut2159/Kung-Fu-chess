package com.chessgame;

/**
 * Cell / תא
 *
 * תפקיד: זוג (row, col). נשאר package-private - בשימוש רק בין
 * CommandParser ל-GameEngine (שתיהן באותה חבילת שורש).
 */
final class Cell {
    final int row;
    final int col;

    Cell(int row, int col) {
        this.row = row;
        this.col = col;
    }
}
