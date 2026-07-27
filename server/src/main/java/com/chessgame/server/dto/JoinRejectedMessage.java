package com.chessgame.server.dto;

/**
 * Sent when a /app/join is refused.
 *
 * Two fields exist purely so the browser can tell who this concerns:
 *
 * - {@code type} separates it from {@link MoveRejectedMessage}, which travels
 *   on the same topic. Without it the client has to guess from which fields
 *   happen to be missing, which breaks the moment either record gains a field.
 * - {@code username} names the rejected player. The errors topic is shared by
 *   everyone in the room, so without it every occupant reacts to one player's
 *   failed join.
 */
public record JoinRejectedMessage(String type, String username, String reason, String occupiedRoomId) {

    public static final String TYPE = "JOIN_REJECTED";

    public static JoinRejectedMessage of(String username, String reason, String occupiedRoomId) {
        return new JoinRejectedMessage(TYPE, username, reason, occupiedRoomId);
    }
}
