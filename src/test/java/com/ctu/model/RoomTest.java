package com.ctu.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Room Entity Unit Test Suite")
class RoomTest {

    @Test
    @DisplayName("Kiem tra getter, setter va khoi tao Room")
    void testRoomConstructorAndProperties() {
        Room room = new Room("R01", 300000.0, true);

        assertEquals("R01", room.getRoomId());
        assertEquals(300000.0, room.getPricePerNight(), 0.001);
        assertTrue(room.isAvailable());

        room.setAvailable(false);
        assertFalse(room.isAvailable());
    }
}
