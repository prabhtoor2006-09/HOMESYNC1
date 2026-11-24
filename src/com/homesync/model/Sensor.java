package com.homesync.model;

public abstract class Sensor {

    private int sensorId;
    private String sensorName;
    private String roomName;
    private double currentReading;
    private boolean active;

    // Constructor
    public Sensor(int sensorId, String sensorName,
            String roomName) {

        this.sensorId = sensorId;
        this.sensorName = sensorName;
        this.roomName = roomName;
        this.currentReading = 0.0;
        this.active = true;
    }

    // Getters
    public int getSensorId() {
        return sensorId;
    }

    public String getSensorName() {
        return sensorName;
    }

    public String getRoomName() {
        return roomName;
    }

    public double getCurrentReading() {
        return currentReading;
    }

    public boolean isActive() {
        return active;
    }

    // Setters
    public void setCurrentReading(double reading) {
        this.currentReading = reading;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Abstract methods
    public abstract void updateReading();

    public abstract void displayDetails();

    // Common method
    public void displayStatus() {
        System.out.println("Sensor Status: "
                + (active ? "Active" : "Inactive"));
    }
}
