package com.chessgame.engine;

import com.chessgame.model.Position;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * שומרת "כוונת-תנועה עתידית" אחת לכל כלי (ממופה לפי התא הנוכחי שלו),
 * שתתבצע אוטומטית ברגע שהקירור של הכלי יסתיים. בקשה חדשה דורסת ישנה,
 * ומהלך לא-חוקי מוחק אותה (ראו GameEngine.handlePremoveRequest).
 */
final class PremoveManager {
    private final Map<Position, Position> pending = new HashMap<>();

    void set(Position source, Position destination) {
        pending.put(source, destination);
    }

    void clear(Position source) {
        pending.remove(source);
    }

    Optional<Position> get(Position source) {
        return Optional.ofNullable(pending.get(source));
    }
}
