package com.homesync.service;

import com.homesync.model.Device;
import com.homesync.utils.FileManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * DeviceRepository - saves and loads the EXISTING Device objects.
 *
 * File: data/devices.csv      Format (4 fields):  id,type,name,status
 * Example:                    1,Light,Living Room Light,ON
 *
 * "type" is the class name (Light, AC, Fan or Device). On loading, the
 * existing DeviceFactory is used to build the right object. No new Device
 * class is created here.
 */
public class DeviceRepository {

    private static final int FIELD_COUNT = 4;

    private final String filePath;

    public DeviceRepository() {
        this(FileManager.DEVICES_FILE);
    }

    public DeviceRepository(String filePath) {
        this.filePath = filePath;
    }

    // ---------------------------------------------------------------- SAVE

    /** Saves one device: updates its line if the ID exists, otherwise appends. */
    public boolean saveDevice(Device device) {
        if (device == null) {
            return false;
        }
        String line = toLine(device);
        String key = String.valueOf(device.getDeviceId());

        if (FileManager.containsKey(filePath, key)) {
            return FileManager.updateLine(filePath, key, line);   // no duplicate IDs
        }
        return FileManager.appendLine(filePath, line);
    }

    /** Replaces the whole file with the given devices (removed devices disappear). */
    public boolean saveAll(List<Device> devices) {
        List<String> lines = new ArrayList<>();
        for (Device device : devices) {
            lines.add(toLine(device));
        }
        return FileManager.writeLines(filePath, lines);
    }

    // ---------------------------------------------------------------- LOAD

    /** Loads every valid device. Invalid or duplicate lines are skipped. */
    public ArrayList<Device> loadDevices() {
        ArrayList<Device> devices = new ArrayList<>();
        Set<Integer> usedIds = new HashSet<>();
        int skipped = 0;

        for (String line : FileManager.readLines(filePath)) {
            Device device = parseDevice(line);
            if (device == null || !usedIds.add(device.getDeviceId())) {
                skipped++;
            } else {
                devices.add(device);
            }
        }

        if (skipped > 0) {
            System.out.println("Device file: skipped " + skipped + " invalid or duplicate record(s).");
        }
        return devices;
    }

    /** Puts saved devices into a DeviceManager (IDs already there are not added again). */
    public int restoreInto(DeviceManager manager) {
        int restored = 0;
        for (Device device : loadDevices()) {
            if (manager.searchDevice(device.getDeviceId()) == null) {
                manager.addDevice(device);
                restored++;
            }
        }
        return restored;
    }

    // -------------------------------------------------------------- SEARCH

    public Device findDevice(int deviceId) {
        String line = FileManager.findLine(filePath, String.valueOf(deviceId));
        return (line == null) ? null : parseDevice(line);
    }

    public ArrayList<Device> searchByName(String keyword) {
        ArrayList<Device> matches = new ArrayList<>();
        for (String line : FileManager.searchLines(filePath, 2, keyword)) {
            Device device = parseDevice(line);
            if (device != null) {
                matches.add(device);
            }
        }
        return matches;
    }

    // ------------------------------------------------------ UPDATE / DELETE

    public boolean updateDevice(Device device) {
        return FileManager.updateLine(filePath, String.valueOf(device.getDeviceId()), toLine(device));
    }

    public boolean deleteDevice(int deviceId) {
        return FileManager.deleteLine(filePath, String.valueOf(deviceId));
    }

    public int countSavedDevices() {
        return FileManager.countLines(filePath);
    }

    // ------------------------------------------------------------- HELPERS

    private String toLine(Device device) {
        return device.getDeviceId() + "," + device.getClass().getSimpleName() + ","
                + FileManager.cleanText(device.getDeviceName()) + "," + (device.isOn() ? "ON" : "OFF");
    }

    /** Returns null when the line is invalid. */
    private Device parseDevice(String line) {
        if (!FileManager.isValidRecord(line, FIELD_COUNT)) {
            return null;
        }

        String[] fields = FileManager.splitFields(line);
        int id;
        try {
            id = Integer.parseInt(fields[0]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (id <= 0) {
            return null;
        }

        // Existing DeviceFactory codes: 1 = Light, 2 = AC, 3 = Fan
        Device device;
        switch (fields[1]) {
            case "Light":
                device = DeviceFactory.createDevice(1, id, fields[2]);
                break;
            case "AC":
                device = DeviceFactory.createDevice(2, id, fields[2]);
                break;
            case "Fan":
                device = DeviceFactory.createDevice(3, id, fields[2]);
                break;
            default:
                device = new Device(id, fields[2]);
        }

        if (fields[3].equalsIgnoreCase("ON")) {
            device.turnOn();     // restores the saved status
        }
        return device;
    }
}
