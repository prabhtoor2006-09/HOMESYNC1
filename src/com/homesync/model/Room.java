
package com.homesync.model;

import java.util.ArrayList;

public class Room {

    private String roomName;
    private ArrayList<Device> devices;

    public Room(String roomName) {
        this.roomName = roomName;
        this.devices = new ArrayList<>();
    }

    public String getRoomName() {
        return roomName;
    }

    public void addDevice(Device device) {

        for (Device existing : devices) {
            if (existing.getDeviceId() == device.getDeviceId()) {
                System.out.println("Device already assigned to this room.");
                return;
            }
        }

        devices.add(device);
        System.out.println(device.getDeviceName() + " assigned to " + roomName);
    }

    public void removeDevice(int id) {
        devices.removeIf(device -> device.getDeviceId() == id);
    }

    public void displayRoomDetails() {

        System.out.println("\nRoom: " + roomName);

        if (devices.isEmpty()) {
            System.out.println("No devices assigned.");
            return;
        }

        for (Device device : devices) {
            device.displayDetails();
            System.out.println("----------------");
        }
    }
}
