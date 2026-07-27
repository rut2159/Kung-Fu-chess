package com.chessgame.server.controller;

import com.chessgame.server.dto.CreateRoomResponse;
import com.chessgame.server.dto.MoveHistoryEntryMessage;
import com.chessgame.server.game.GameRoom;
import com.chessgame.server.game.RoomRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock
    private RoomRegistry roomRegistry;

    @Mock
    private GameRoom room;

    private RoomController newController() {
        return new RoomController(roomRegistry);
    }

    @Test
    void create_returnsTheNewRoomsId() {
        RoomController controller = newController();
        when(roomRegistry.createRoom()).thenReturn(room);
        when(room.roomId()).thenReturn("K7F2QX");

        CreateRoomResponse response = controller.create();

        assertEquals("K7F2QX", response.roomId());
    }

    @Test
    void exists_returns200_whenTheRoomIsOpen() {
        RoomController controller = newController();
        when(roomRegistry.exists("K7F2QX")).thenReturn(true);

        ResponseEntity<Void> response = controller.exists("K7F2QX");

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void exists_returns404_whenTheRoomIsUnknown() {
        RoomController controller = newController();
        when(roomRegistry.exists("ZZZZZZ")).thenReturn(false);

        ResponseEntity<Void> response = controller.exists("ZZZZZZ");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void moves_returnsTheRoomsHistory_soARefreshDoesNotLoseIt() {
        RoomController controller = newController();
        MoveHistoryEntryMessage entry = new MoveHistoryEntryMessage("WHITE", "e4", "0:01", 1000L);
        when(roomRegistry.find("K7F2QX")).thenReturn(Optional.of(room));
        when(room.moveHistory()).thenReturn(List.of(entry));

        ResponseEntity<List<MoveHistoryEntryMessage>> response = controller.moves("K7F2QX");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains(entry));
    }

    @Test
    void moves_returns404_whenTheRoomIsUnknown() {
        RoomController controller = newController();
        when(roomRegistry.find("ZZZZZZ")).thenReturn(Optional.empty());

        ResponseEntity<List<MoveHistoryEntryMessage>> response = controller.moves("ZZZZZZ");

        assertEquals(404, response.getStatusCode().value());
    }
}
