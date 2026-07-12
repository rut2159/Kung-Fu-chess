package com.chessgame.model;

/**
 * Piece / כלי
 *
 * תפקיד: מייצג כלי שחמט בודד. לכלי יש id יציב, צבע, סוג, המשבצת שהוא
 * נמצא בה כרגע, ומצב מחזור-חיים.
 *
 * state הוא דגל lifecycle בלבד. הוא לא שומר מסלול, יעד, זמן שחלף,
 * מהירות, אינטרפולציה, או לוגיקת הגעה/נחיתה - כל אלה שייכים ל-Motion
 * ול-RealTimeArbiter (מחלקות שנבנה בהמשך).
 *
 * שימו לב ל-AIRBORNE: זו תוספת מעבר למסמך הדרישות הרשמי, לבקשת
 * המשתמשת - מייצגת כלי שביצע קפיצה (jump). זה *לא* תת-סוג של MOVING:
 * כלי MOVING נמצא בדרך ליעד; כלי AIRBORNE נשאר באותה משבצת בדיוק,
 * רק "לא זמין לבחירה רגילה" עד שהוא נוחת מעצמו או לוכד כלי אויב שמגיע.
 *
 * כלי אף פעם לא יודע כלום על: הרנדרר, קליקי עכבר, פיקסלים, או תחביר
 * טקסט-טסט.
 */
public final class Piece {

    public enum Color {
        WHITE, BLACK
    }

    public enum Kind {
        KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
    }

    public enum State {
        IDLE, MOVING, AIRBORNE, CAPTURED
    }

    private final String id;
    private final Color color;
    private Kind kind;
    private Position cell;
    private State state;

    public Piece(String id, Color color, Kind kind, Position cell) {
        this.id = id;
        this.color = color;
        this.kind = kind;
        this.cell = cell;
        this.state = State.IDLE;
    }

    public String id() {
        return id;
    }

    public Color color() {
        return color;
    }

    public Kind kind() {
        return kind;
    }

    public Position cell() {
        return cell;
    }

    public State state() {
        return state;
    }

    /**
     * מעדכן את המשבצת הנוכחית של הכלי. נקרא רק על ידי מי שאחראי
     * בפועל על תזוזת כלים (RealTimeArbiter בזמן resolve של הגעה) -
     * לא על ידי Renderer, Controller, או שום קוד אחר.
     */
    public void setCell(Position cell) {
        this.cell = cell;
    }

    /**
     * מעדכן את מצב מחזור-החיים של הכלי (idle/moving/airborne/captured).
     * שוב - נקרא רק על ידי RealTimeArbiter, לא על ידי אף אחד אחר.
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * הופך רגלי למלכה. נקרא רק על ידי ArrivalResolver, ורק כשהרגלי
     * מגיע לשורה-האחרונה עבור הצבע שלו (הכתרה). שומרים על אותו
     * אובייקט-כלי בדיוק (לא יוצרים כלי חדש) - כי Motion כבר מחזיק
     * רפרנס ישיר לכלי הזה, ואנחנו לא רוצים "לנתק" את הרפרנס הזה
     * באמצע resolve של הגעה.
     */
    public void promoteToQueen() {
        this.kind = Kind.QUEEN;
    }

    public boolean isSameColorAs(Piece other) {
        return this.color == other.color;
    }

    public boolean isEnemyOf(Piece other) {
        return this.color != other.color;
    }

    @Override
    public String toString() {
        return color + " " + kind + " #" + id + " at " + cell + " (" + state + ")";
    }
}