package com.homesync.model;

import java.util.Random;

public class MotionSensor extends Sensor {

    private Random random = new Random();
    private boolean motionDetected;

    // Constructor
    public MotionSensor(int sensorId, String sensorName,
            String roomName) {
        super(sensorId, sensorName, roomName);
        this.motionDetected = false;
    }

    // Simulate motion detection
    @Override
    public void updateReading() {

        if (!isActive()) {
            System.out.println("Motion sensor is inactive.");
            return;
        }

        motionDetected = random.nextBoolean();

        setCurrentReading(motionDetected ? 1 : 0);

        System.out.println("Motion detected: "
                + motionDetected);
    }

    // Return motion detection status
    public boolean isMotionDetected() {
        return motionDetected;
    }

    // Display sensor details
    @Override
    public void displayDetails() {

        System.out.println("\n--- Motion Sensor ---");
        System.out.println("Sensor ID: " + getSensorId());
        System.out.println("Sensor Name: " + getSensorName());
        System.out.println("Room: " + getRoomName());
        System.out.println("Motion Detected: "
                + motionDetected);

        displayStatus();
    }
}
