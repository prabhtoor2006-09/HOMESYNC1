package com.homesync.service;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.homesync.model.Device;
import com.homesync.model.EnergyRecord;
import com.homesync.utils.FileManager;

/**
 * EnergyManager - keeps all simulated energy records.
 *
 * Collections:
 *   ArrayList<EnergyRecord>        - every record in order
 *   HashMap<Integer, Double>       - total kWh per device ID (device-wise energy)
 *
 * Multithreading:
 *   * A background "EnergyMonitor" thread (MonitorTask) adds records for every
 *     device that is ON, while the user keeps using the menu.
 *   * The menu thread and the monitor thread share the same lists, so every
 *     method that touches them is "synchronized" (prevents race conditions).
 *
 * Simple methods for other modules:
 *   recordUsage(device, hours), getTotalEnergy(), getEnergyForDevice(id)
 */
public class EnergyManager {

    // Typical power ratings (watts) used because Device has no wattage field.
    // They can be changed at run time with setPowerRating(...).
    private static final double LIGHT_WATTS = 12.0;   // LED bulb
    private static final double AC_WATTS = 1500.0;    // 1.5 ton split AC
    private static final double FAN_WATTS = 75.0;     // ceiling fan
    public static final double DEFAULT_POWER_WATTS = 100.0; // any other device

    public static final long DEFAULT_INTERVAL_MILLIS = 2000;
    public static final double DEFAULT_SIMULATED_HOURS_PER_TICK = 0.5;

    private final ArrayList<EnergyRecord> records = new ArrayList<>();
    private final HashMap<Integer, Double> deviceEnergy = new HashMap<>();
    private final HashMap<String, Double> powerRatings = new HashMap<>();

    // Separate lock for the thread fields, so stopMonitoring() can join()
    // without holding the lock the monitor thread needs (avoids deadlock).
    private final Object monitorLock = new Object();
    private Thread monitorThread;
    private volatile int monitorTicks;   // written by monitor thread, read by menu thread

    public EnergyManager() {
        powerRatings.put("Light", LIGHT_WATTS);
        powerRatings.put("AC", AC_WATTS);
        powerRatings.put("Fan", FAN_WATTS);
    }

    // ------------------------------------------------------- POWER RATINGS

    public synchronized void setPowerRating(String deviceType, double watts) {
        if (deviceType == null || deviceType.trim().isEmpty() || watts < 0) {
            throw new IllegalArgumentException("Need a device type and a non-negative wattage.");
        }
        powerRatings.put(deviceType.trim(), watts);
    }

    /** Power rating by type name, e.g. "AC". */
    public synchronized double getPowerRating(String deviceType) {
        Double watts = powerRatings.get(deviceType);
        return (watts != null) ? watts : DEFAULT_POWER_WATTS;
    }

    /** Overloaded: power rating of a Device (its class name is the type). */
    public double getPowerRating(Device device) {
        return getPowerRating(device.getClass().getSimpleName());
    }

    // ------------------------------------------------------------- RECORDS

    public synchronized void addRecord(EnergyRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Record cannot be null.");
        }
        records.add(record);

        double previous = deviceEnergy.getOrDefault(record.getDeviceId(), 0.0);
        deviceEnergy.put(record.getDeviceId(), previous + record.getEnergyKwh());
    }

    /** Records usage of a Device for some hours, using its power rating. */
    public EnergyRecord recordUsage(Device device, double hours) {
        EnergyRecord record = new EnergyRecord(device.getDeviceId(), device.getDeviceName(),
                getPowerRating(device), hours);
        addRecord(record);
        return record;
    }

    /** Overloaded: record usage when the details are already known. */
    public EnergyRecord recordUsage(int deviceId, String deviceName, double watts, double hours) {
        EnergyRecord record = new EnergyRecord(deviceId, deviceName, watts, hours);
        addRecord(record);
        return record;
    }

    /** Adds one record for every device that is currently ON. Returns how many. */
    public int recordUsageForOnDevices(DeviceManager deviceManager, double hours) {
        List<Device> snapshot;
        try {
            snapshot = new ArrayList<>(deviceManager.getDevices());
        } catch (ConcurrentModificationException | ArrayIndexOutOfBoundsException e) {
            // The menu thread changed the device list while we were copying it.
            // Skip this tick; the next tick will work.
            return 0;
        }

        int count = 0;
        for (Device device : snapshot) {
            if (device.isOn()) {
                recordUsage(device, hours);
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------- QUERIES

    public synchronized double getTotalEnergy() {
        double total = 0;
        for (EnergyRecord record : records) {
            total += record.getEnergyKwh();
        }
        return total;
    }

    public synchronized double getEnergyForDevice(int deviceId) {
        return deviceEnergy.getOrDefault(deviceId, 0.0);
    }

    /** A copy, so callers cannot change our internal map. */
    public synchronized Map<Integer, Double> getDeviceEnergy() {
        return new HashMap<>(deviceEnergy);
    }

    public synchronized List<EnergyRecord> getRecords() {
        return new ArrayList<>(records);
    }

    public synchronized List<EnergyRecord> getRecordsForDevice(int deviceId) {
        List<EnergyRecord> result = new ArrayList<>();
        for (EnergyRecord record : records) {
            if (record.getDeviceId() == deviceId) {
                result.add(record);
            }
        }
        return result;
    }

    public synchronized int getRecordCount() {
        return records.size();
    }

    /** Name of a device as seen in the records, or "Device <id>". */
    public synchronized String getDeviceName(int deviceId) {
        for (EnergyRecord record : records) {
            if (record.getDeviceId() == deviceId) {
                return record.getDeviceName();
            }
        }
        return "Device " + deviceId;
    }

    /** ID of the device that used the most energy, or -1 if no records. */
    public synchronized int getTopConsumerId() {
        int topId = -1;
        double topEnergy = -1;
        for (Map.Entry<Integer, Double> entry : deviceEnergy.entrySet()) {
            if (entry.getValue() > topEnergy) {
                topEnergy = entry.getValue();
                topId = entry.getKey();
            }
        }
        return topId;
    }

    public synchronized String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Records: ").append(records.size());
        summary.append(" | Devices tracked: ").append(deviceEnergy.size());
        summary.append(" | Total energy: ").append(String.format("%.3f", getTotalEnergy())).append(" kWh");

        int top = getTopConsumerId();
        if (top != -1) {
            summary.append(" | Highest consumer: ").append(getDeviceName(top));
        }
        return summary.toString();
    }

    public synchronized void clearRecords() {
        records.clear();
        deviceEnergy.clear();
    }

    // --------------------------------------------------------- FILE STORAGE

    /** Saves all records to data/energy_records.csv. */
    public synchronized boolean saveToFile() {
        List<String> lines = new ArrayList<>();
        for (EnergyRecord record : records) {
            lines.add(record.toCsvLine());
        }
        return FileManager.writeLines(FileManager.ENERGY_FILE, lines);
    }

    /** Replaces memory with the saved records. Invalid and duplicate lines are skipped. */
    public synchronized int loadFromFile() {
        clearRecords();
        Set<String> seen = new HashSet<>();
        int skipped = 0;

        for (String line : FileManager.readLines(FileManager.ENERGY_FILE)) {
            try {
                EnergyRecord record = EnergyRecord.fromCsvLine(line);
                if (seen.add(record.toCsvLine())) {   // add() is false for a duplicate
                    addRecord(record);
                } else {
                    skipped++;
                }
            } catch (IllegalArgumentException e) {
                skipped++;
            }
        }

        if (skipped > 0) {
            System.out.println("Energy file: skipped " + skipped + " invalid or duplicate record(s).");
        }
        return records.size();
    }

    // ----------------------------------------------- BACKGROUND MONITORING

    /**
     * Starts the background monitoring thread.
     * Every intervalMillis the thread adds simulatedHoursPerTick hours of
     * usage for each device that is ON (simulation - not real metering).
     */
    public boolean startMonitoring(DeviceManager deviceManager, long intervalMillis,
                                   double simulatedHoursPerTick) {
        if (deviceManager == null || intervalMillis <= 0 || simulatedHoursPerTick <= 0) {
            throw new IllegalArgumentException("Interval and hours per tick must be positive.");
        }

        synchronized (monitorLock) {
            if (monitorThread != null && monitorThread.isAlive()) {
                return false;   // already running
            }
            monitorTicks = 0;
            monitorThread = new Thread(new MonitorTask(deviceManager, intervalMillis, simulatedHoursPerTick),
                    "EnergyMonitor");
            monitorThread.setDaemon(true);                      // does not keep the program alive
            monitorThread.setPriority(Thread.MIN_PRIORITY);     // background work (only a hint to the OS)
            monitorThread.start();                              // start() creates a NEW thread that calls run()
            return true;
        }
    }

    /** Overloaded: start with the default settings. */
    public boolean startMonitoring(DeviceManager deviceManager) {
        return startMonitoring(deviceManager, DEFAULT_INTERVAL_MILLIS, DEFAULT_SIMULATED_HOURS_PER_TICK);
    }

    /** Stops the monitoring thread: interrupt() wakes it from sleep(), join() waits for it to finish. */
    public boolean stopMonitoring() {
        Thread threadToStop;
        synchronized (monitorLock) {
            threadToStop = monitorThread;
            monitorThread = null;
        }

        if (threadToStop == null) {
            return false;
        }

        threadToStop.interrupt();
        try {
            threadToStop.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

    public boolean isMonitoring() {
        synchronized (monitorLock) {
            return monitorThread != null && monitorThread.isAlive();
        }
    }

    public int getMonitorTicks() {
        return monitorTicks;
    }

    /** Thread lifecycle state: NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING or TERMINATED. */
    public String getMonitorState() {
        synchronized (monitorLock) {
            return (monitorThread == null) ? "NOT STARTED" : monitorThread.getState().toString();
        }
    }

    /** The job the monitor thread runs. A class that implements Runnable. */
    private class MonitorTask implements Runnable {

        private final DeviceManager deviceManager;
        private final long intervalMillis;
        private final double hoursPerTick;

        MonitorTask(DeviceManager deviceManager, long intervalMillis, double hoursPerTick) {
            this.deviceManager = deviceManager;
            this.intervalMillis = intervalMillis;
            this.hoursPerTick = hoursPerTick;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(intervalMillis);            // thread is in TIMED_WAITING here
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();      // keep the interrupt flag, then leave the loop
                    break;
                }
                recordUsageForOnDevices(deviceManager, hoursPerTick);
                monitorTicks++;   // only this thread writes it; volatile lets the menu thread see it
            }
        }
    }

    // -------------------------------------------------- THREAD DEMONSTRATION

    /**
     * Demonstrates a race condition. Two threads count "meter pulses" on one
     * shared counter. Round 1 has no protection and loses updates; round 2 uses
     * synchronized and always gives the correct answer.
     */
    public void demonstrateRaceCondition() {
        final int pulsesPerThread = 200000;
        int expected = 2 * pulsesPerThread;

        System.out.println("\n--- Race condition demo ---");
        int unsafeResult = countPulses(pulsesPerThread, false, true);
        int safeResult = countPulses(pulsesPerThread, true, false);

        System.out.println("Expected pulses    : " + expected);
        System.out.println("Without synchronized: " + unsafeResult + "  (updates lost = " + (expected - unsafeResult) + ")");
        System.out.println("With synchronized   : " + safeResult + "  (updates lost = " + (expected - safeResult) + ")");
        if (unsafeResult == expected) {
            System.out.println("(This run happened not to collide - race conditions are random. Run it again.)");
        }
    }

    /**
     * counter++ is really THREE steps: read, add one, write back. Here the steps
     * are written out, and yield() now and then lets the other thread run in
     * between (this makes the race visible even on a computer with one CPU core).
     */
    private void addPulse(int[] counter, int step) {
        int current = counter[0];            // 1. read
        if (step % 500 == 0) {
            Thread.yield();                  // another thread may change counter[0] here
        }
        counter[0] = current + 1;            // 2+3. add one and write back
    }

    /** Two threads each count pulses on ONE shared counter. */
    private int countPulses(final int pulsesPerThread, final boolean useLock, boolean showStates) {
        final int[] counter = {0};            // shared by both threads
        final Object counterLock = new Object();

        Runnable meterTask = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < pulsesPerThread; i++) {
                    if (useLock) {
                        synchronized (counterLock) {   // only one thread at a time in this block
                            addPulse(counter, i);
                        }
                    } else {
                        addPulse(counter, i);          // no protection
                    }
                }
            }
        };

        Thread meterA = new Thread(meterTask, "Meter-A");
        Thread meterB = new Thread(meterTask, "Meter-B");

        if (showStates) {
            System.out.println("Meter-A state before start(): " + meterA.getState());   // NEW
        }
        meterA.start();
        meterB.start();
        if (showStates) {
            System.out.println("Meter-A state after start()  : " + meterA.getState());  // RUNNABLE
        }

        try {
            meterA.join();    // wait until the thread has finished
            meterB.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Demo interrupted.");
            return counter[0];
        }
        if (showStates) {
            System.out.println("Meter-A state after join()   : " + meterA.getState());  // TERMINATED
        }
        return counter[0];
    }
}
