package com.chessgame.server.dto;

public record MatchmakingStatusResponse(String status, String roomId) {

    public static MatchmakingStatusResponse waiting() {
        return new MatchmakingStatusResponse("WAITING", null);
    }

    public static MatchmakingStatusResponse matched(String roomId) {
        return new MatchmakingStatusResponse("MATCHED", roomId);
    }

    public static MatchmakingStatusResponse timedOut() {
        return new MatchmakingStatusResponse("TIMED_OUT", null);
    }
}
