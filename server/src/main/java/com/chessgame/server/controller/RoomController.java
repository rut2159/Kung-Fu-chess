package com.chessgame.server.controller;

import com.chessgame.server.dto.CreateRoomResponse;
import com.chessgame.server.dto.MoveHistoryEntryMessage;
import com.chessgame.server.game.GameRoom;
import com.chessgame.server.game.RoomRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRegistry roomRegistry;

    public RoomController(RoomRegistry roomRegistry) {
        this.roomRegistry = roomRegistry;
    }

    @PostMapping
    public CreateRoomResponse create() {
        GameRoom room = roomRegistry.createRoom();
        return new CreateRoomResponse(room.roomId());
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<Void> exists(@PathVariable("roomId") String roomId) {
        return roomRegistry.exists(roomId)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{roomId}/moves")
    public ResponseEntity<List<MoveHistoryEntryMessage>> moves(@PathVariable("roomId") String roomId) {
        return roomRegistry.find(roomId)
                .map(room -> ResponseEntity.ok(room.moveHistory()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
