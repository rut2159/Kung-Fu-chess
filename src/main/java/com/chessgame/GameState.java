package com.chessgame;

/**
 * GameState / מצב משחק
 *
 * תפקיד: מייצג באיזה שלב פרסינג הקלט אנחנו נמצאים. נשאר package-private -
 * בשימוש רק בתוך GameEngine (אותה חבילה), אין סיבה שאף חבילה אחרת תדע עליו.
 */
enum GameState {
    INIT,
    PARSING_BOARD,
    PARSING_COMMANDS
}
