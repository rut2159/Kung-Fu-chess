package com.chessgame.server.game;

public final class Topics {

    private static final String ROOM_PREFIX = "/topic/room/";

    public static final String PONG = "/topic/pong";

    private Topics() {
    }

    public static String gameState(String roomId) {
        return ROOM_PREFIX + roomId + "/game";
    }

    public static String moveHistory(String roomId) {
        return ROOM_PREFIX + roomId + "/moves";
    }

    public static String errors(String roomId) {
        return ROOM_PREFIX + roomId + "/errors";
    }

    public static String closed(String roomId) {
        return ROOM_PREFIX + roomId + "/closed";
    }
}
