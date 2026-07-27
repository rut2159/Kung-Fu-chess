package com.chessgame.server.game;

import com.chessgame.io.StandardBoard;
import com.chessgame.server.dto.RoomClosedMessage;
import com.chessgame.server.service.RatingService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomRegistry {

    private static final Logger log = LoggerFactory.getLogger(RoomRegistry.class);
    private static final int MAX_ID_ATTEMPTS = 20;
    private static final long DESERTED_GRACE_MS = 20_000;

    private final RatingService ratingService;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> roomBySession = new ConcurrentHashMap<>();
    private final Map<String, String> usernameBySession = new ConcurrentHashMap<>();
    private final Map<String, Long> desertedSince = new ConcurrentHashMap<>();

    public RoomRegistry(RatingService ratingService, SimpMessagingTemplate messagingTemplate) {
        this.ratingService = ratingService;
        this.messagingTemplate = messagingTemplate;
    }

    @PreDestroy
    void shutdown() {
        rooms.values().forEach(GameRoom::shutdown);
        rooms.clear();
    }

    public synchronized GameRoom createRoom() {
        for (int attempt = 0; attempt < MAX_ID_ATTEMPTS; attempt++) {
            String candidate = RoomId.generate();
            if (!rooms.containsKey(candidate)) {
                GameRoom room = new GameRoom(candidate, StandardBoard.create(),
                        ratingService, messagingTemplate);
                rooms.put(candidate, room);
                desertedSince.put(candidate, System.currentTimeMillis());
                log.info("room {} created", candidate);
                return room;
            }
        }
        throw new IllegalStateException("Could not allocate a free room id");
    }

    public Optional<GameRoom> find(String roomId) {
        return Optional.ofNullable(rooms.get(RoomId.normalise(roomId)));
    }

    public boolean exists(String roomId) {
        return rooms.containsKey(RoomId.normalise(roomId));
    }

    public Optional<GameRoom> roomForSession(String sessionId) {
        String roomId = roomBySession.get(sessionId);
        return roomId == null ? Optional.empty() : Optional.ofNullable(rooms.get(roomId));
    }

    public Optional<String> usernameForSession(String sessionId) {
        return Optional.ofNullable(usernameBySession.get(sessionId));
    }

    public List<String> openRoomIds() {
        return List.copyOf(rooms.keySet());
    }

    public synchronized JoinOutcome join(String roomId, String sessionId, String username) {
        String normalised = RoomId.normalise(roomId);
        GameRoom room = rooms.get(normalised);
        if (room == null) {
            log.warn("join rejected: room {} does not exist. open rooms: {}", normalised, rooms.keySet());
            return JoinOutcome.notFound();
        }

        Optional<String> elsewhere = roomOfPlayer(username);
        if (elsewhere.isPresent() && !elsewhere.get().equals(normalised)) {
            log.info("join rejected: {} is already in room {}", username, elsewhere.get());
            return JoinOutcome.alreadyInAnotherRoom(elsewhere.get());
        }

        roomBySession.put(sessionId, normalised);
        usernameBySession.put(sessionId, username);
        desertedSince.remove(normalised);
        Seats.Role role = room.join(username);
        log.info("room {}: {} joined as {}", normalised, username, role);
        return JoinOutcome.joined(room, role);
    }

    public synchronized void leave(String sessionId) {
        String roomId = roomBySession.remove(sessionId);
        String username = usernameBySession.remove(sessionId);
        if (roomId == null || username == null) {
            return;
        }
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return;
        }
        if (hasActiveSession(roomId, username)) {
            return;
        }
        log.info("room {}: {} lost their last session", roomId, username);
        room.playerLeft(username);
    }

    private Optional<String> roomOfPlayer(String username) {
        return roomBySession.entrySet().stream()
                .filter(entry -> username.equals(usernameBySession.get(entry.getKey())))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private boolean hasActiveSession(String roomId, String username) {
        return roomBySession.entrySet().stream()
                .filter(entry -> entry.getValue().equals(roomId))
                .anyMatch(entry -> username.equals(usernameBySession.get(entry.getKey())));
    }

    public void tickAll(int milliseconds) {
        for (GameRoom room : rooms.values()) {
            room.tick(milliseconds);
        }
        closeDesertedRooms();
    }

    void closeDesertedRooms() {
        closeDesertedRoomsAt(System.currentTimeMillis());
    }

    synchronized void closeDesertedRoomsAt(long now) {
        for (GameRoom room : new ArrayList<>(rooms.values())) {
            if (anyoneConnected(room)) {
                desertedSince.remove(room.roomId());
                continue;
            }
            Long since = desertedSince.putIfAbsent(room.roomId(), now);
            if (since != null && now - since >= DESERTED_GRACE_MS) {
                close(room);
            }
        }
    }

    private boolean anyoneConnected(GameRoom room) {
        boolean white = room.whiteUsername()
                .map(name -> hasActiveSession(room.roomId(), name)).orElse(false);
        boolean black = room.blackUsername()
                .map(name -> hasActiveSession(room.roomId(), name)).orElse(false);
        return white || black;
    }

    private void close(GameRoom room) {
        String roomId = room.roomId();
        rooms.remove(roomId);
        desertedSince.remove(roomId);

        List<String> sessions = roomBySession.entrySet().stream()
                .filter(entry -> entry.getValue().equals(roomId))
                .map(Map.Entry::getKey)
                .toList();
        sessions.forEach(roomBySession::remove);
        sessions.forEach(usernameBySession::remove);

        messagingTemplate.convertAndSend(Topics.closed(roomId), new RoomClosedMessage(roomId));
        room.shutdown();
        log.info("room {} closed after {} ms with nobody seated", roomId, DESERTED_GRACE_MS);
    }

    public record JoinOutcome(Status status, GameRoom room, Seats.Role role, String occupiedRoomId) {

        public enum Status { JOINED, ROOM_NOT_FOUND, ALREADY_IN_ANOTHER_ROOM }

        static JoinOutcome joined(GameRoom room, Seats.Role role) {
            return new JoinOutcome(Status.JOINED, room, role, null);
        }

        static JoinOutcome notFound() {
            return new JoinOutcome(Status.ROOM_NOT_FOUND, null, null, null);
        }

        static JoinOutcome alreadyInAnotherRoom(String occupiedRoomId) {
            return new JoinOutcome(Status.ALREADY_IN_ANOTHER_ROOM, null, null, occupiedRoomId);
        }

        public boolean isJoined() {
            return status == Status.JOINED;
        }
    }
}
