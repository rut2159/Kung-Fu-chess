package com.chessgame.engine.premove;

import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PremoveManager {

    /**
     * שומר גם את הכלי שביקש את הפרימוב, לא רק את המיקום - כדי שפרימוב
     * "יתום" (של כלי שנהרג בזמן ה-cooldown, לפני שהפרימוב שלו הופעל
     * ונוקה כרגיל) לא "ידבק" בטעות לכלי אחר-לגמרי שיגיע מאוחר יותר
     * לאותו ריבוע ויכנס בעצמו ל-cooldown. ראו get() למקום שבו זה נבדק.
     */
    private record Entry(Piece piece, Position destination) {
    }

    private final Map<Position, Entry> pending = new HashMap<>();

    public void set(Position source, Piece piece, Position destination) {
        pending.put(source, new Entry(piece, destination));
    }

    public void clear(Position source) {
        pending.remove(source);
    }

    /**
     * מחזיר את יעד הפרימוב השמור עבור source, אך ורק אם currentOccupant
     * הוא בדיוק אותו אובייקט-כלי שביקש את הפרימוב במקור (זהות, לא רק
     * מיקום). אם הכלי המקורי כבר לא שם (נהרג, למשל) - הרשומה מטופלת
     * כלא-רלוונטית יותר, מנוקה, ומוחזר Optional ריק, כדי שהיא לא תישאר
     * תקועה ותופעל בטעות על כלי אחר שיגיע לאותו ריבוע בעתיד.
     */
    public Optional<Position> get(Position source, Piece currentOccupant) {
        Entry entry = pending.get(source);
        if (entry == null) {
            return Optional.empty();
        }
        if (currentOccupant == null || entry.piece() != currentOccupant) {
            pending.remove(source);
            return Optional.empty();
        }
        return Optional.of(entry.destination());
    }
}
