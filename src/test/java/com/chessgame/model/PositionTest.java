package com.chessgame.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PositionTest / טסטים ל-Position
 *
 * 3 הטסטים המדויקים שהמסמך דורש עבור Position: שוויון, אי-שוויון,
 * וייצוג קריא. Position הוא value object טהור - אין state, אין
 * setup נדרש, כל טסט עצמאי לגמרי.
 */
class PositionTest {

    @Test
    void twoPositionsWithSameRowAndCol_areEqual() {
        // דרישה 1 מהמסמך: שתי משבצות עם אותה שורה ואותה עמודה - שוות
        Position a = new Position(2, 3);
        Position b = new Position(2, 3);

        assertEquals(a, b);
        // בונוס: hashCode חייב להיות שווה גם הוא, אחרת Position לא
        // יעבוד נכון כמפתח ב-Map/Set (זה בדיוק מה ש-Board.occupancy
        // ו-legalDestinations() מסתמכים עליו)
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void positionsWithDifferentRow_areNotEqual() {
        // דרישה 2 מהמסמך: שורה שונה - לא שוות
        Position a = new Position(2, 3);
        Position c = new Position(5, 3);

        assertNotEquals(a, c);
    }

    @Test
    void positionsWithDifferentCol_areNotEqual() {
        // דרישה 2 מהמסמך, החלק השני: עמודה שונה - לא שוות
        Position a = new Position(2, 3);
        Position d = new Position(2, 9);

        assertNotEquals(a, d);
    }

    @Test
    void toString_producesReadableRepresentation() {
        // דרישה 3 מהמסמך: ייצוג קריא - חשוב כדי שכישלון-טסט יראה
        // "(2,3)" ולא כתובת-זיכרון לא-קריאה כמו Position@1a2b3c
        Position p = new Position(2, 3);

        assertEquals("(2,3)", p.toString());
    }

    @Test
    void aPositionIsEqualToItself() {
        // מקרה-קצה קטן: אובייקט תמיד שווה לעצמו (reflexivity) -
        // בודק את מסלול ה-"this == other" בתוך equals()
        Position p = new Position(0, 0);

        assertEquals(p, p);
    }
}
