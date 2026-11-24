package com.homesync.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * EnergyRecord - one entry of simulated energy usage for one device.
 *
 * Formula used:  kWh = watts x hours / 1000
 *
 * Fields are private (encapsulation). The record cannot be changed after it
 * is created, so it is safe to share between threads without locking.
 */
public class EnergyRecord {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int CSV_FIELD_COUNT = 6;

    private final int deviceId;
    private final String deviceName;
    private final double powerWatts;
    private final double hoursUsed;
    private final double energyKwh;
    private final LocalDateTime timestamp;

    /** Uses the current date and time as the timestamp. */
    public EnergyRecord(int deviceId, String deviceName, double powerWatts, double hoursUsed) {
        this(deviceId, deviceName, powerWatts, hoursUsed, LocalDateTime.now().withNano(0));
    }

    /** Used when loading old records from a file. */
    public EnergyRecord(int deviceId, String deviceName, double powerWatts,
                        double hoursUsed, LocalDateTime timestamp) {

        if (deviceId <= 0) {
            throw new IllegalArgumentException("Device ID must be positive.");
        }
        if (deviceName == null || deviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Device name cannot be empty.");
        }
        if (powerWatts < 0) {
            throw new IllegalArgumentException("Power rating cannot be negative.");
        }
        if (hoursUsed < 0) {
            throw new IllegalArgumentException("Usage hours cannot be negative.");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp is required.");
        }

        this.deviceId = deviceId;
        this.deviceName = deviceName.trim().replace(",", " ");
        this.powerWatts = powerWatts;
        this.hoursUsed = hoursUsed;
        this.energyKwh = calculateKwh(powerWatts, hoursUsed);
        this.timestamp = timestamp;
    }

    /** The energy formula in one place: kWh = watts x hours / 1000. */
    public static double calculateKwh(double powerWatts, double hoursUsed) {
        return (powerWatts * hoursUsed) / 1000.0;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public double getPowerWatts() {
        return powerWatts;
    }

    public double getHoursUsed() {
        return hoursUsed;
    }

    public double getEnergyKwh() {
        return energyKwh;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /** Format: deviceId,deviceName,powerWatts,hoursUsed,energyKwh,timestamp */
    public String toCsvLine() {
        return deviceId + "," + deviceName + "," + powerWatts + "," + hoursUsed
                + "," + energyKwh + "," + timestamp.format(FORMAT);
    }

    /** Rebuilds a record from a CSV line. Throws IllegalArgumentException if invalid. */
    public static EnergyRecord fromCsvLine(String line) {
        if (line == null) {
            throw new IllegalArgumentException("Record is empty.");
        }

        String[] fields = line.split(",", -1);
        if (fields.length != CSV_FIELD_COUNT) {
            throw new IllegalArgumentException("Expected " + CSV_FIELD_COUNT + " fields but found " + fields.length);
        }

        try {
            int id = Integer.parseInt(fields[0].trim());
            String name = fields[1].trim();
            double watts = Double.parseDouble(fields[2].trim());
            double hours = Double.parseDouble(fields[3].trim());
            LocalDateTime time = LocalDateTime.parse(fields[5].trim(), FORMAT);
            // fields[4] (stored kWh) is recalculated, so a wrong value cannot sneak in
            return new EnergyRecord(id, name, watts, hours, time);
        } catch (NumberFormatException | DateTimeParseException e) {
            // multi-catch: both problems mean "bad data in the file"
            throw new IllegalArgumentException("Invalid number or date in record: " + line);
        }
    }

    @Override
    public String toString() {
        return String.format("[%s] Device %d (%s): %.1f W for %.2f h = %.3f kWh",
                timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                deviceId, deviceName, powerWatts, hoursUsed, energyKwh);
    }
}
