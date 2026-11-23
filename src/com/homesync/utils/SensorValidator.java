package com.homesync.utils;

import java.util.ArrayList;
import com.homesync.model.Sensor;

public class SensorValidator {

    // Check whether sensor ID is positive
    public static boolean isValidId(int id) {
        return id > 0;
    }

    // Check whether sensor ID is unique
    public static boolean isUniqueId(
            int id, ArrayList<Sensor> sensors) {

        for (Sensor sensor : sensors) {
            if (sensor.getSensorId() == id) {
                return false;
            }
        }

        return true;
    }

    // Validate sensor reading
    public static boolean isValidReading(double reading) {
        return Double.isFinite(reading) && reading >= 0;
    }

    // Validate sensor name
    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty();
    }
}
