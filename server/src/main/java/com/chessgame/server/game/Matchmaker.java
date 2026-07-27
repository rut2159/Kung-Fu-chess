package com.chessgame.server.game;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pairs waiting players whose rating is within {@link #RATING_RANGE} of each
 * other. Waiting time is a virtual clock advanced by {@link #tick(int)},
 * driven by the same scheduler as {@link RoomRegistry#tickAll(int)}, so a
 * test can simulate the one-minute timeout without a real sleep.
 */
@Service
public class Matchmaker {

    public enum Status { WAITING, MATCHED, TIMED_OUT }

    public record Outcome(Status status, String roomId) {
        static Outcome waiting() {
            return new Outcome(Status.WAITING, null);
        }

        static Outcome matched(String roomId) {
            return new Outcome(Status.MATCHED, roomId);
        }

        static Outcome timedOut() {
            return new Outcome(Status.TIMED_OUT, null);
        }
    }

    private static final int RATING_RANGE = 100;
    private static final long SEEK_TIMEOUT_MS = 60_000;

    private static final class Ticket {
        final int rating;
        volatile long waitedMs;
        volatile String roomId;

        Ticket(int rating) {
            this.rating = rating;
        }
    }

    private final RoomRegistry roomRegistry;
    private final Map<String, Ticket> waiting = new ConcurrentHashMap<>();

    public Matchmaker(RoomRegistry roomRegistry) {
        this.roomRegistry = roomRegistry;
    }

    public synchronized Outcome poll(String username, int rating) {
        Ticket mine = waiting.get(username);
        if (mine != null && mine.roomId != null) {
            waiting.remove(username);
            return Outcome.matched(mine.roomId);
        }
        if (mine == null) {
            mine = new Ticket(rating);
            waiting.put(username, mine);
        }

        Optional<Map.Entry<String, Ticket>> opponent = findOpponent(username, rating);
        if (opponent.isPresent()) {
            GameRoom room = roomRegistry.createRoom();
            opponent.get().getValue().roomId = room.roomId();
            waiting.remove(username);
            return Outcome.matched(room.roomId());
        }

        if (mine.waitedMs >= SEEK_TIMEOUT_MS) {
            waiting.remove(username);
            return Outcome.timedOut();
        }
        return Outcome.waiting();
    }

    public synchronized void tick(int milliseconds) {
        for (Ticket ticket : waiting.values()) {
            if (ticket.roomId == null) {
                ticket.waitedMs += milliseconds;
            }
        }
    }

    private Optional<Map.Entry<String, Ticket>> findOpponent(String username, int rating) {
        return waiting.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(username))
                .filter(entry -> entry.getValue().roomId == null)
                .filter(entry -> Math.abs(entry.getValue().rating - rating) <= RATING_RANGE)
                .min(Comparator.comparingInt(entry -> Math.abs(entry.getValue().rating - rating)));
    }
}
