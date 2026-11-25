package com.homesync.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.homesync.model.BrightnessSensor;
import com.homesync.model.MotionSensor;
import com.homesync.model.Sensor;
import com.homesync.model.TemperatureSensor;
import com.homesync.utils.FileManager;
import com.homesync.utils.SensorValidator;

/**
 * SensorRepository - saves and loads Anshul's EXISTING Sensor objects.
 *
 * File   : data/sensors.csv
 * Format : id,type,name,room,reading,active,lastSaved          (7 fields)
 * Example: 1,TemperatureSensor,Hall Thermostat,Hall,29.5,true,2026-11-25T14:03:22
 *
 * "type" is the class name (TemperatureSensor, MotionSensor, BrightnessSensor).
 * On loading, the matching Anshul class is created with its own constructor,
 * then setCurrentReading() and setActive() restore the saved values.
 * No new Sensor class is created here.
 *
 * Known limitation: MotionSensor keeps its "motionDetected" flag private with
 * no setter, so after loading, isMotionDetected() is false until the next
 * updateReading(). The saved numeric reading (1 or 0) is still restored.
 */
public class SensorRepository {

    private static final int FIELD_COUNT = 7;

    private final String filePath;

    public SensorRepository() {
        this(FileManager.SENSORS_FILE);
    }

    public SensorRepository(String filePath) {
        this.filePath = filePath;
    }

    // ---------------------------------------------------------------- SAVE

    /** Saves one sensor: updates its line if the ID exists, otherwise appends. */
    public boolean saveSensor(Sensor sensor) {
        if (sensor == null || !SensorValidator.isValidId(sensor.getSensorId())) {
            return false;
        }
        String line = toLine(sensor);
        String key = String.valueOf(sensor.getSensorId());

        if (FileManager.containsKey(filePath, key)) {
            return FileManager.updateLine(filePath, key, line);   // no duplicate IDs
        }
        return FileManager.appendLine(filePath, line);
    }

    /** Replaces the whole file with the given sensors (removed sensors disappear). */
    public boolean saveAll(ArrayList<Sensor> sensors) {
        ArrayList<String> lines = new ArrayList<>();
        for (Sensor sensor : sensors) {
            lines.add(toLine(sensor));
        }
        return FileManager.writeLines(filePath, lines);
    }

    // ---------------------------------------------------------------- LOAD

    /** Loads every valid sensor. Invalid or duplicate lines are skipped. */
    public ArrayList<Sensor> loadSensors() {
        ArrayList<Sensor> sensors = new ArrayList<>();
        Set<Integer> usedIds = new HashSet<>();
        int skipped = 0;

        for (String line : FileManager.readLines(filePath)) {
            Sensor sensor = parseSensor(line);
            if (sensor == null || !usedIds.add(sensor.getSensorId())) {
                skipped++;
            } else {
                sensors.add(sensor);
            }
        }

        if (skipped > 0) {
            System.out.println("Sensor file: skipped " + skipped + " invalid or duplicate record(s).");
        }
        return sensors;
    }

    /** Puts saved sensors into a SensorManager (IDs already there are not added again). */
    public int restoreInto(SensorManager manager) {
        int restored = 0;
        for (Sensor sensor : loadSensors()) {
            if (manager.searchSensor(sensor.getSensorId()) == null) {
                manager.addSensor(sensor);
                restored++;
            }
        }
        return restored;
    }

    // -------------------------------------------------------------- SEARCH

    public Sensor findSensor(int sensorId) {
        String line = FileManager.findLine(filePath, String.valueOf(sensorId));
        return (line == null) ? null : parseSensor(line);
    }

    /** Sensors whose saved line contains the keyword (name, room, type ...). */
    public ArrayList<Sensor> searchSensors(String keyword) {
        ArrayList<Sensor> matches = new ArrayList<>();
        for (String line : FileManager.searchLines(filePath, keyword)) {
            Sensor sensor = parseSensor(line);
            if (sensor != null) {
                matches.add(sensor);
            }
        }
        return matches;
    }

    // ------------------------------------------------------ UPDATE / DELETE

    public boolean updateSensor(Sensor sensor) {
        return FileManager.updateLine(filePath, String.valueOf(sensor.getSensorId()), toLine(sensor));
    }

    /** Changes only the saved reading of one sensor. */
    public boolean updateReading(int sensorId, double newReading) {
        Sensor sensor = findSensor(sensorId);
        if (sensor == null || !Double.isFinite(newReading)) {
            return false;
        }
        sensor.setCurrentReading(newReading);
        return updateSensor(sensor);
    }

    public boolean deleteSensor(int sensorId) {
        return FileManager.deleteLine(filePath, String.valueOf(sensorId));
    }

    public int countSavedSensors() {
        return FileManager.countLines(filePath);
    }

    // ------------------------------------------------------------- HELPERS

    private String toLine(Sensor sensor) {
        String time = LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return sensor.getSensorId() + "," + sensor.getClass().getSimpleName() + ","
                + FileManager.cleanText(sensor.getSensorName()) + ","
                + FileManager.cleanText(sensor.getRoomName()) + ","
                + sensor.getCurrentReading() + "," + sensor.isActive() + "," + time;
    }

    /** Returns null when the line is invalid. */
    private Sensor parseSensor(String line) {
        if (!FileManager.isValidRecord(line, FIELD_COUNT)) {
            return null;
        }

        String[] fields = FileManager.splitFields(line);
        int id;
        double reading;
        try {
            id = Integer.parseInt(fields[0]);
            reading = Double.parseDouble(fields[4]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (!SensorValidator.isValidId(id) || !Double.isFinite(reading)) {
            return null;
        }

        Sensor sensor;
        switch (fields[1]) {
            case "TemperatureSensor":
                sensor = new TemperatureSensor(id, fields[2], fields[3]);
                break;
            case "MotionSensor":
                sensor = new MotionSensor(id, fields[2], fields[3]);
                break;
            case "BrightnessSensor":
                sensor = new BrightnessSensor(id, fields[2], fields[3]);
                break;
            default:
                return null;      // unknown sensor type
        }

        sensor.setCurrentReading(reading);
        sensor.setActive(fields[5].equalsIgnoreCase("true"));
        return sensor;
    }
}
