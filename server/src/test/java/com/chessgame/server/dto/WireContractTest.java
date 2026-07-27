package com.chessgame.server.dto;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * נועל את שמות השדות שהלקוח בדפדפן קורא.
 *
 * game.js ניגש לשדות האלה בשם, ואין מהדר שמקשר בין הצדדים. שינוי שם של
 * רכיב ב-record ייראה כמו ריפקטורינג תמים בצד ה-Java, ויישבר בשקט רק
 * בזמן ריצה בדפדפן. הטסט הזה הופך את זה לכישלון בילד.
 *
 * הבדיקה היא על רכיבי ה-record ולא על JSON מסוריאליזציה, כי אלה בדיוק
 * המפתחות שנוצרים - וכך אין תלות בגרסת ספריית הסריאליזציה. אם אי פעם
 * יתווסף כאן שינוי שם מפורש באנוטציה, הטסט הזה יפסיק לכסות אותו.
 */
class WireContractTest {

    private static Set<String> componentsOf(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
    }

    @Test
    void gameStateMessage_fieldsMatchTheBrowserClient() {
        assertEquals(
                Set.of("width", "height", "pieces", "gameOver", "winner",
                        "whiteUsername", "blackUsername", "whiteScore", "blackScore",
                        "disconnectedUsername", "resignInSeconds"),
                componentsOf(GameStateMessage.class));
    }

    @Test
    void pieceDto_fieldsMatchTheBrowserClient() {
        assertEquals(
                Set.of("id", "color", "kind", "row", "col",
                        "displayRow", "displayCol", "state", "cooldownRemaining", "hasPremove"),
                componentsOf(PieceDto.class));
    }

    @Test
    void moveHistoryEntryMessage_fieldsMatchTheBrowserClient() {
        assertEquals(
                Set.of("color", "notation", "time", "timestampMs"),
                componentsOf(MoveHistoryEntryMessage.class));
    }

    @Test
    void moveRejectedMessage_fieldsMatchTheBrowserClient() {
        assertEquals(
                Set.of("username", "reason"),
                componentsOf(MoveRejectedMessage.class));
    }

    @Test
    void moveCommand_fieldsMatchTheBrowserClient() {
        assertEquals(
                Set.of("fromRow", "fromCol", "toRow", "toCol"),
                componentsOf(MoveCommand.class));
    }

    @Test
    void jumpCommand_fieldsMatchTheBrowserClient() {
        assertEquals(
                Set.of("row", "col"),
                componentsOf(JumpCommand.class));
    }
}
