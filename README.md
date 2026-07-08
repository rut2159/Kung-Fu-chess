# Kung Fu Chess

פרויקט Java פשוט שמיישם מנגנון לחיצות לוח ושינויי כלי שחמט.

## מבנה הפרויקט

- `pom.xml` - תצורת Maven לפרויקט ו-JUnit
- `src/main/java/com/kungfuchess` - קוד המקור של המשחק
- `src/test/java/com/kungfuchess` - מבחני JUnit

## איך לבנות את הפרויקט

```bash
mvn clean compile
```

## איך להריץ

```bash
mvn exec:java -Dexec.mainClass="com.kungfuchess.Main"
```

או אם אין לך Maven:

```bash
javac -d target/classes src/main/java/com/kungfuchess/*.java
java -cp target/classes com.kungfuchess.Main
```

## איך להריץ את המבחנים

```bash
mvn test
```

> שים לב: סביבת העבודה הנוכחית לא כוללת Maven מותקן, אבל הקבצים מוכנים לעבודה עם Maven.
