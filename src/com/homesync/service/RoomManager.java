
package com.homesync.service;

import java.util.HashMap;
import com.homesync.model.Room;
import com.homesync.model.Device;

public class RoomManager {

    private HashMap<String, Room> rooms = new HashMap<>();

    public void addRoom(String name) {

        if (name == null || name.trim().isEmpty()) {
            System.out.println("Room name cannot be empty.");
            return;
        }

        String key = name.trim().toLowerCase();

        if (rooms.containsKey(key)) {
            System.out.println("Room already exists.");
            return;
        }

        rooms.put(key, new Room(name.trim()));
        System.out.println(name.trim() + " created successfully.");
    }

    public void assignDeviceToRoom(Device device, String roomName) {

        Room room = rooms.get(roomName.trim().toLowerCase());

        if (room == null) {
            System.out.println("Room not found. Create the room first.");
            return;
        }

        room.addDevice(device);
    }

    public void removeDeviceFromRooms(int id) {

        for (Room room : rooms.values()) {
            room.removeDevice(id);
        }
    }

    public void displayRooms() {

        if (rooms.isEmpty()) {
            System.out.println("No rooms created.");
            return;
        }

        for (Room room : rooms.values()) {
            room.displayRoomDetails();
        }
    }
}
