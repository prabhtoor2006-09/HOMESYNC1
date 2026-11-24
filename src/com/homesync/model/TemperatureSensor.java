package com.homesync.model;

import java.util.Random;

public class TemperatureSensor extends Sensor {

    private Random random = new Random();

    // Constructor
    public TemperatureSensor(int sensorId, String sensorName,
            String roomName) {
        super(sensorId, sensorName, roomName);
    }

    // Generate simulated temperature
    @Override
    public void updateReading() {

        if (!isActive()) {
            System.out.println("Temperature sensor is inactive.");
            return;
        }

        double temperature = 18 + random.nextInt(181) / 10.0;

        setCurrentReading(temperature);

        System.out.println("Temperature updated: "
                + temperature + " °C");
    }

    // Display temperature sensor details
    @Override
    public void displayDetails() {

        System.out.println("\n--- Temperature Sensor ---");
        System.out.println("Sensor ID: " + getSensorId());
        System.out.println("Sensor Name: " + getSensorName());
        System.out.println("Room: " + getRoomName());
        System.out.println("Temperature: "
                + getCurrentReading() + " °C");

        displayStatus();
    }
}
