package com.homesync.service;

import java.util.ArrayList;
import java.util.HashMap;

import com.homesync.model.Sensor;
import com.homesync.model.TemperatureSensor;
import com.homesync.model.BrightnessSensor;
import com.homesync.model.MotionSensor;
import com.homesync.utils.SensorValidator;

public class SensorManager {

    private ArrayList<Sensor> sensors = new ArrayList<>();

    // HashMap for faster sensor searching by ID
    private HashMap<Integer, Sensor> sensorMap = new HashMap<>();

    // Return all sensors
    public ArrayList<Sensor> getSensors() {
        return sensors;
    }

    // Add a new sensor
    public void addSensor(Sensor sensor) {

        if (sensor == null) {
            System.out.println("Invalid sensor.");
            return;
        }

        int id = sensor.getSensorId();

        if (!SensorValidator.isValidId(id)) {
            System.out.println("Invalid sensor ID.");
            return;
        }

        if (!SensorValidator.isUniqueId(id, sensors)) {
            System.out.println("Sensor ID already exists.");
            return;
        }

        if (!SensorValidator.isValidName(sensor.getSensorName())
                || !SensorValidator.isValidName(sensor.getRoomName())) {
            System.out.println("Sensor name and room name cannot be empty.");
            return;
        }

        sensors.add(sensor);
        sensorMap.put(id, sensor);

        System.out.println(sensor.getSensorName()
                + " added successfully.");
    }

    // Search sensor by ID
    public Sensor searchSensor(int id) {
        return sensorMap.get(id);
    }

    // Remove sensor
    public void removeSensor(int id) {

        Sensor sensor = sensorMap.remove(id);

        if (sensor != null) {
            sensors.remove(sensor);
            System.out.println("Sensor removed successfully.");
        } else {
            System.out.println("Sensor not found.");
        }
    }

    // Display all sensors
    public void displaySensors() {

        if (sensors.isEmpty()) {
            System.out.println("No sensors available.");
            return;
        }

        for (Sensor sensor : sensors) {
            sensor.displayDetails();
            System.out.println("----------------");
        }
    }

    // Update a sensor's simulated reading
    public void updateSensorReading(int id) {

        Sensor sensor = searchSensor(id);

        if (sensor != null) {
            sensor.updateReading();
        } else {
            System.out.println("Sensor not found.");
        }
    }

    // Get sensors assigned to a particular room
    public ArrayList<Sensor> getSensorsByRoom(String roomName) {

        ArrayList<Sensor> roomSensors = new ArrayList<>();

        if (roomName == null) {
            return roomSensors;
        }

        for (Sensor sensor : sensors) {
            if (sensor.getRoomName().equalsIgnoreCase(roomName.trim())) {
                roomSensors.add(sensor);
            }
        }

        return roomSensors;
    }

    // Retrieve temperature reading for a room
    public Double getTemperatureReading(String roomName) {

        for (Sensor sensor : getSensorsByRoom(roomName)) {
            if (sensor instanceof TemperatureSensor) {
                return sensor.getCurrentReading();
            }
        }

        return null;
    }

    // Retrieve brightness reading for a room
    public Double getBrightnessReading(String roomName) {

        for (Sensor sensor : getSensorsByRoom(roomName)) {
            if (sensor instanceof BrightnessSensor) {
                return sensor.getCurrentReading();
            }
        }

        return null;
    }

    // Retrieve motion status for a room
    public Boolean isMotionDetected(String roomName) {

        for (Sensor sensor : getSensorsByRoom(roomName)) {
            if (sensor instanceof MotionSensor) {
                return ((MotionSensor) sensor).isMotionDetected();
            }
        }

        return null;
    }

    // Display sensor statistics
    public void displayStatistics() {

        int temperatureCount = 0;
        int brightnessCount = 0;
        int motionCount = 0;
        int activeCount = 0;

        for (Sensor sensor : sensors) {

            if (sensor instanceof TemperatureSensor) {
                temperatureCount++;
            } else if (sensor instanceof BrightnessSensor) {
                brightnessCount++;
            } else if (sensor instanceof MotionSensor) {
                motionCount++;
            }

            if (sensor.isActive()) {
                activeCount++;
            }
        }

        System.out.println("\n===== SENSOR STATISTICS =====");
        System.out.println("Total sensors: " + sensors.size());
        System.out.println("Temperature sensors: " + temperatureCount);
        System.out.println("Brightness sensors: " + brightnessCount);
        System.out.println("Motion sensors: " + motionCount);
        System.out.println("Active sensors: " + activeCount);
        System.out.println("Inactive sensors: "
                + (sensors.size() - activeCount));
    }
}
