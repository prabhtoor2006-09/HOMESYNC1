package com.homesync.service;

import com.homesync.utils.FileManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * ActivityLog - records important events, in memory AND in data/activity_log.txt.
 *
 * HOW OTHER MODULES USE IT (one line):
 *     ActivityLog.getInstance().log("SENSOR_UPDATE", "Hall temperature = 29.5");
 * or one of the ready-made helpers, e.g.
 *     ActivityLog.getInstance().logAutomationTriggered("Turn fan ON above 30C");
 *
 * File line format:  yyyy-MM-dd HH:mm:ss | EVENT_TYPE | details
 *
 * Collection used: LinkedList. New events are added at the end and the oldest
 * is removed from the front when the list is full - both are cheap on a LinkedList.
 *
 * Methods are synchronized because several threads may log at the same time.
 */
public class ActivityLog {

    private static final int MAX_HISTORY_IN_MEMORY = 100;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static ActivityLog sharedInstance;

    private final LinkedList<String> history = new LinkedList<>();
    private final String filePath;

    public ActivityLog() {
        this(FileManager.ACTIVITY_LOG_FILE);
    }

    public ActivityLog(String filePath) {
        this.filePath = filePath;
        FileManager.createFile(filePath);
        loadHistoryFromFile();
    }

    /** One shared log for the whole application. */
    public static synchronized ActivityLog getInstance() {
        if (sharedInstance == null) {
            sharedInstance = new ActivityLog();
        }
        return sharedInstance;
    }

    // ------------------------------------------------------------- LOGGING

    public synchronized void log(String eventType, String details) {
        String entry = LocalDateTime.now().format(TIME_FORMAT) + " | " + eventType + " | " + details;

        history.addLast(entry);
        if (history.size() > MAX_HISTORY_IN_MEMORY) {
            history.removeFirst();
        }
        FileManager.appendLine(filePath, entry);
    }

    /** Overloaded: general message with type INFO. */
    public void log(String message) {
        log("INFO", message);
    }

    public void logDeviceAdded(int deviceId, String deviceName) {
        log("DEVICE_ADDED", deviceName + " (ID " + deviceId + ") added");
    }

    public void logDeviceRemoved(int deviceId) {
        log("DEVICE_REMOVED", "Device ID " + deviceId + " removed");
    }

    public void logDeviceOn(int deviceId, String deviceName) {
        log("DEVICE_ON", deviceName + " (ID " + deviceId + ") turned ON");
    }

    public void logDeviceOff(int deviceId, String deviceName) {
        log("DEVICE_OFF", deviceName + " (ID " + deviceId + ") turned OFF");
    }

    public void logSensorUpdated(String sensorName, double reading) {
        log("SENSOR_UPDATE", sensorName + " reading = " + reading);
    }

    public void logAutomationTriggered(String ruleName) {
        log("AUTOMATION", "Rule triggered: " + ruleName);
    }

    public void logEnergyReportGenerated() {
        log("ENERGY_REPORT", "Energy report generated");
    }

    public void logBackupCreated(String backupName) {
        log("BACKUP_CREATED", "Backup " + backupName);
    }

    public void logDataRestored(String backupName) {
        log("DATA_RESTORED", "Data restored from " + backupName);
    }

    // ------------------------------------------------------------- READING

    /** The newest n entries, newest first (uses a ListIterator going backwards). */
    public synchronized List<String> getRecent(int count) {
        List<String> recent = new ArrayList<>();
        ListIterator<String> iterator = history.listIterator(history.size());

        while (iterator.hasPrevious() && recent.size() < count) {
            recent.add(iterator.previous());
        }
        return recent;
    }

    public synchronized int getHistorySize() {
        return history.size();
    }

    /** Searches the saved log FILE (not just memory). */
    public List<String> search(String keyword) {
        return FileManager.searchLines(filePath, keyword);
    }

    public void displayRecent(int count) {
        List<String> recent = getRecent(count);
        System.out.println("\n===== ACTIVITY LOG (latest " + count + ") =====");
        if (recent.isEmpty()) {
            System.out.println("No activity recorded yet.");
            return;
        }
        for (String entry : recent) {
            System.out.println(entry);
        }
    }

    /** Loads the last saved entries so the log continues after a restart. */
    private synchronized void loadHistoryFromFile() {
        history.clear();
        for (String line : FileManager.readLines(filePath)) {
            history.addLast(line);
            if (history.size() > MAX_HISTORY_IN_MEMORY) {
                history.removeFirst();
            }
        }
    }
}
