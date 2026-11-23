
package com.homesync.service;

import java.util.ArrayList;
import com.homesync.model.Device;
import com.homesync.utils.DeviceValidator;

public class DeviceManager {

    private ArrayList<Device> devices = new ArrayList<>();
    public ArrayList<Device> getDevices() {
        return devices;
}
    // Add a new device
public void addDevice(Device device) {

    int id = device.getDeviceId();

    if (!DeviceValidator.isValidId(id)) {
        System.out.println("Invalid device ID. ID must be positive.");
        return;
    }

    if (!DeviceValidator.isUniqueId(id, devices)) {
        System.out.println("Device ID already exists.");
        return;
    }

    devices.add(device);
    System.out.println(device.getDeviceName() + " added successfully.");
}

    // Display all devices
    public void displayDevices() {
        if (devices.isEmpty()) {
            System.out.println("No devices available.");
            return;
        }

        for (Device device : devices) {
            device.displayDetails();
            System.out.println("----------------");
        }
    }

    // Search device using ID
    public Device searchDevice(int id) {
        for (Device device : devices) {
            if (device.getDeviceId() == id) {
                return device;
            }
        }
        return null;
    }

    // Turn device ON
    public void turnDeviceOn(int id) {
        Device device = searchDevice(id);

        if (device != null) {
            device.turnOn();
        } else {
            System.out.println("Device not found.");
        }
    }

    // Turn device OFF
    public void turnDeviceOff(int id) {
        Device device = searchDevice(id);

        if (device != null) {
            device.turnOff();
        } else {
            System.out.println("Device not found.");
        }
    }

    // Remove device
    public void removeDevice(int id) {
        Device device = searchDevice(id);

        if (device != null) {
            devices.remove(device);
            System.out.println("Device removed successfully.");
        } else {
            System.out.println("Device not found.");
        }
    }

    // Display device statistics
    public void displayStatistics() {
        int active = 0;

        for (Device device : devices) {
            if (device.isOn()) {
                active++;
            }
        }

        System.out.println("Total devices: " + devices.size());
        System.out.println("Devices ON: " + active);
        System.out.println("Devices OFF: " + (devices.size() - active));
    }
}
