package com.homesync.model;

import java.util.Random;

public class BrightnessSensor extends Sensor {

    private Random random = new Random();

    // Constructor
    public BrightnessSensor(int sensorId, String sensorName,
            String roomName) {
        super(sensorId, sensorName, roomName);
    }

    // Generate simulated brightness
    @Override
    public void updateReading() {

        if (!isActive()) {
            System.out.println("Brightness sensor is inactive.");
            return;
        }

        int brightness = random.nextInt(101);

        setCurrentReading(brightness);

        System.out.println("Brightness updated: "
                + brightness + "%");
    }

    // Display brightness sensor details
    @Override
    public void displayDetails() {

        System.out.println("\n--- Brightness Sensor ---");
        System.out.println("Sensor ID: " + getSensorId());
        System.out.println("Sensor Name: " + getSensorName());
        System.out.println("Room: " + getRoomName());
        System.out.println("Brightness: "
                + getCurrentReading() + "%");

        displayStatus();
    }
}
