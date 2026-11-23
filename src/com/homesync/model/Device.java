
package com.homesync.model;

import com.homesync.interfaces.Switchable;

public class Device implements Switchable {

    private int deviceId;
    private String deviceName;
    private boolean isOn;

    public Device(int deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.isOn = false;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public boolean isOn() {
        return isOn;
    }

    @Override
    public void turnOn() {
        isOn = true;
        System.out.println(deviceName + " is ON");
    }

    @Override
    public void turnOff() {
        isOn = false;
        System.out.println(deviceName + " is OFF");
    }

    public void displayDetails() {
        System.out.println("Device ID: " + deviceId);
        System.out.println("Device Name: " + deviceName);
        System.out.println("Status: " + (isOn ? "ON" : "OFF"));
    }
}
