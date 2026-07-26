package com.chessgame.server.dto;

/**
 * שורה אחת בטבלת המהלכים.
 *
 * timestampMs הוא שעון המשחק הגולמי באלפיות. time הוא אותו ערך מפורמט
 * לתצוגה. הגולמי נחוץ ללקוח כדי למיין ולזהות כפילויות כשהוא מרכיב מחדש
 * את הטבלה אחרי רענון או חיבור-מחדש - השוואת מחרוזות מפורמטות שבירה.
 */
public record MoveHistoryEntryMessage(String color, String notation, String time, long timestampMs) {
}
