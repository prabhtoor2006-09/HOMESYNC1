package com.homesync.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.homesync.utils.FileManager;

/**
 * SensorRepository - saves and loads sensor records.
 *
 * INTEGRATION CONTRACT (Anshul's Sensor classes were not in the repository
 * when this was written, so NO Sensor class is used or duplicated here):
 *
 *   File   : data/sensors.csv
 *   Format : id,name,type,reading,lastUpdated        (5 fields)
 *   Example: 1,Hall Thermostat,Temperature,29.5,2026-11-25T14:03:22
 *
 *   Sensor module calls (all values come from Anshul's Sensor getters):
 *     saveSensor(int id, String name, String type, double reading)  -> boolean
 *     updateReading(int id, double newReading)                      -> boolean
 *     findSensor(int id)                                            -> String[] or null
 *     loadSensors()                                                 -> List<String[]>
 *     searchSensors(String keyword)                                 -> List<String[]>
 *     deleteSensor(int id)                                          -> boolean
 *
 *   Each String[] uses the INDEX_ constants below (INDEX_ID, INDEX_NAME, ...).
 *   Once Anshul's Sensor class exists, a one-line overload such as
 *   saveSensor(Sensor s) can call saveSensor(s.getId(), s.getName(), ...).
 */
public class SensorRepository {

    public static final int INDEX_ID = 0;
    public static final int INDEX_NAME = 1;
    public static final int INDEX_TYPE = 2;
    public static final int INDEX_READING = 3;
    public static final int INDEX_UPDATED = 4;

    private static final int FIELD_COUNT = 5;

    private final String filePath;

    public SensorRepository() {
        this(FileManager.SENSORS_FILE);
    }

    public SensorRepository(String filePath) {
        this.filePath = filePath;
    }

    /** Saves a sensor: updates it if the ID exists, otherwise appends it. */
    public boolean saveSensor(int id, String name, String type, double reading) {
        if (id <= 0 || isBlank(name) || isBlank(type)) {
            System.out.println("Invalid sensor: ID must be positive, name and type are required.");
            return false;
        }

        String line = buildLine(id, name, type, reading);
        String key = String.valueOf(id);

        if (FileManager.containsKey(filePath, key)) {
            return FileManager.updateLine(filePath, key, line);
        }
        return FileManager.appendLine(filePath, line);
    }

    /** Loads all valid sensors; invalid lines are skipped. */
    public List<String[]> loadSensors() {
        List<String[]> sensors = new ArrayList<>();
        int skipped = 0;

        for (String line : FileManager.readLines(filePath)) {
            if (isValidSensorLine(line)) {
                sensors.add(FileManager.splitFields(line));
            } else {
                skipped++;
            }
        }

        if (skipped > 0) {
            System.out.println("Sensor file: skipped " + skipped + " invalid record(s).");
        }
        return sensors;
    }

    public String[] findSensor(int id) {
        String line = FileManager.findLine(filePath, String.valueOf(id));
        return (line != null && isValidSensorLine(line)) ? FileManager.splitFields(line) : null;
    }

    public List<String[]> searchSensors(String keyword) {
        List<String[]> matches = new ArrayList<>();
        for (String line : FileManager.searchLines(filePath, keyword)) {
            if (isValidSensorLine(line)) {
                matches.add(FileManager.splitFields(line));
            }
        }
        return matches;
    }

    /** Changes only the reading (and time) of an existing sensor. */
    public boolean updateReading(int id, double newReading) {
        String[] sensor = findSensor(id);
        if (sensor == null) {
            return false;
        }
        return FileManager.updateLine(filePath, String.valueOf(id),
                buildLine(id, sensor[INDEX_NAME], sensor[INDEX_TYPE], newReading));
    }

    public boolean deleteSensor(int id) {
        return FileManager.deleteLine(filePath, String.valueOf(id));
    }

    public void displaySensors() {
        List<String[]> sensors = loadSensors();
        System.out.println("\n===== SAVED SENSORS =====");
        if (sensors.isEmpty()) {
            System.out.println("No sensors saved.");
            return;
        }
        for (String[] s : sensors) {
            System.out.println("Sensor " + s[INDEX_ID] + " | " + s[INDEX_NAME] + " | " + s[INDEX_TYPE]
                    + " | reading " + s[INDEX_READING] + " | updated " + s[INDEX_UPDATED]);
        }
    }

    // ------------------------------------------------------------- HELPERS

    private String buildLine(int id, String name, String type, double reading) {
        String time = LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return id + "," + FileManager.cleanText(name) + "," + FileManager.cleanText(type) + "," + reading + "," + time;
    }

    private boolean isValidSensorLine(String line) {
        if (!FileManager.isValidRecord(line, FIELD_COUNT)) {
            return false;
        }
        String[] fields = FileManager.splitFields(line);
        try {
            return Integer.parseInt(fields[INDEX_ID]) > 0 && !Double.isNaN(Double.parseDouble(fields[INDEX_READING]));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
