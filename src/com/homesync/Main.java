
package com.homesync;

import com.homesync.model.AC;
import com.homesync.model.AutomationRule;
import com.homesync.model.BrightnessRule;
import com.homesync.model.BrightnessSensor;
import com.homesync.model.Device;
import com.homesync.model.EnergyRecord;
import com.homesync.model.Fan;
import com.homesync.model.Light;
import com.homesync.model.MotionRule;
import com.homesync.model.MotionSensor;
import com.homesync.model.Sensor;
import com.homesync.model.TemperatureRule;
import com.homesync.model.TemperatureSensor;
import com.homesync.service.ActivityLog;
import com.homesync.service.AutomationEngine;
import com.homesync.service.BackupManager;
import com.homesync.service.DeviceFactory;
import com.homesync.service.DeviceManager;
import com.homesync.service.DeviceReport;
import com.homesync.service.DeviceRepository;
import com.homesync.service.EnergyManager;
import com.homesync.service.EnergyReport;
import com.homesync.service.RoomManager;
import com.homesync.service.RuleManager;
import com.homesync.service.SensorManager;
import com.homesync.service.SensorRepository;
import com.homesync.utils.TariffCalculator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Main {

    public static int getNumber(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.println("Invalid input! Please enter a number.");
            sc.next();
            System.out.print("Try again: ");
        }
        return sc.nextInt();
    }

    public static double getDecimal(Scanner sc) {
        while (!sc.hasNextDouble()) {
            System.out.println("Invalid input! Please enter a number.");
            sc.next();
            System.out.print("Try again: ");
        }
        return sc.nextDouble();
    }

    public static void saveAllData(DeviceManager manager, DeviceRepository deviceRepository,
                                   EnergyManager energyManager, SensorManager sensorManager,
                                   SensorRepository sensorRepository) {
        deviceRepository.saveAll(manager.getDevices());
        sensorRepository.saveAll(sensorManager.getSensors());
        energyManager.saveToFile();
        System.out.println("Devices, sensors and energy records saved to the data folder.");
    }

    public static void addNewSensor(Scanner sc, SensorManager sensorManager, ActivityLog activityLog) {

        System.out.println("\nSelect Sensor Type:");
        System.out.println("1. Temperature");
        System.out.println("2. Motion");
        System.out.println("3. Brightness");

        System.out.print("Enter sensor type: ");
        int type = getNumber(sc);

        if (type < 1 || type > 3) {
            System.out.println("Invalid sensor type.");
            return;
        }

        System.out.print("Enter Sensor ID: ");
        int id = getNumber(sc);

        sc.nextLine();

        System.out.print("Enter Sensor Name: ");
        String name = sc.nextLine().trim();

        System.out.print("Enter Room Name: ");
        String room = sc.nextLine().trim();

        Sensor sensor;
        if (type == 1) {
            sensor = new TemperatureSensor(id, name, room);
        } else if (type == 2) {
            sensor = new MotionSensor(id, name, room);
        } else {
            sensor = new BrightnessSensor(id, name, room);
        }

        sensorManager.addSensor(sensor);
        if (sensorManager.searchSensor(id) == sensor) {
            activityLog.log("SENSOR_ADDED", name + " (ID " + id + ") in " + room);
        }
    }

    public static void addAutomationRule(Scanner sc, DeviceManager manager, RuleManager ruleManager,
                                         AutomationEngine engine, ActivityLog activityLog) {

        System.out.println("\nSelect Rule Type:");
        System.out.println("1. Temperature rule (controls an AC)");
        System.out.println("2. Motion rule (turns a light ON)");
        System.out.println("3. Brightness rule (controls a light)");

        System.out.print("Enter rule type: ");
        int type = getNumber(sc);

        if (type < 1 || type > 3) {
            System.out.println("Invalid rule type.");
            return;
        }

        System.out.print("Enter Rule ID: ");
        int ruleId = getNumber(sc);

        sc.nextLine();

        System.out.print("Enter Rule Name: ");
        String ruleName = sc.nextLine().trim();

        if (ruleName.isEmpty()) {
            System.out.println("Rule name cannot be empty.");
            return;
        }

        System.out.print("Enter ID of the device this rule controls: ");
        int controlledId = getNumber(sc);

        if (manager.searchDevice(controlledId) == null) {
            System.out.println("Device not found. Add the device first.");
            return;
        }
        if (ruleManager.searchRule(ruleId) != null) {
            System.out.println("Rule ID already exists.");
            return;
        }

        AutomationRule rule;
        if (type == 1) {
            rule = new TemperatureRule(ruleId, ruleName, controlledId);
        } else if (type == 2) {
            rule = new MotionRule(ruleId, ruleName, controlledId);
        } else {
            rule = new BrightnessRule(ruleId, ruleName, controlledId);
        }

        ruleManager.addRule(rule);   // RuleManager keeps the rules (enable/disable/search)
        engine.addRule(rule);        // AutomationEngine evaluates the same rule object
        activityLog.log("RULE_ADDED", ruleName + " (ID " + ruleId + ")");
    }

    public static void runAutomation(Scanner sc, DeviceManager manager, SensorManager sensorManager,
                                     AutomationEngine engine, ActivityLog activityLog) {

        sc.nextLine();
        System.out.print("Enter room name whose sensors should be used: ");
        String room = sc.nextLine().trim();

        Double temperature = sensorManager.getTemperatureReading(room);
        Double brightness = sensorManager.getBrightnessReading(room);
        Boolean motion = sensorManager.isMotionDetected(room);

        if (temperature == null && brightness == null && motion == null) {
            System.out.println("No sensors found in room: " + room);
            return;
        }

        System.out.println("Readings used -> Temperature: " + (temperature == null ? "no sensor" : temperature)
                + " | Brightness: " + (brightness == null ? "no sensor" : brightness)
                + " | Motion: " + (motion == null ? "no sensor" : motion));

        // A missing sensor gives NaN / false, which makes its rules do nothing.
        double temperatureValue = (temperature == null) ? Double.NaN : temperature;
        double brightnessValue = (brightness == null) ? Double.NaN : brightness;
        boolean motionValue = (motion != null) && motion;

        HashMap<Integer, Boolean> statusBefore = new HashMap<>();
        for (Device device : manager.getDevices()) {
            statusBefore.put(device.getDeviceId(), device.isOn());
        }

        engine.evaluateRules(temperatureValue, brightnessValue, motionValue);

        int changed = 0;
        for (Device device : manager.getDevices()) {
            Boolean wasOn = statusBefore.get(device.getDeviceId());
            if (wasOn != null && wasOn != device.isOn()) {
                changed++;
                activityLog.log("AUTOMATION", device.getDeviceName() + " (ID " + device.getDeviceId()
                        + ") turned " + (device.isOn() ? "ON" : "OFF") + " by automation");
            }
        }
        activityLog.log("AUTOMATION", "Rules evaluated for room " + room + ", devices changed: " + changed);
        System.out.println("Automation finished. Devices changed: " + changed);
    }

    public static void addNewDevice(Scanner sc, DeviceManager manager) {

        System.out.println("\nSelect Device Type:");
        System.out.println("1. Light");
        System.out.println("2. AC");
        System.out.println("3. Fan");

        System.out.print("Enter device type: ");
        int type = getNumber(sc);

        if (type < 1 || type > 3) {
            System.out.println("Invalid device type.");
            return;
        }

        System.out.print("Enter Device ID: ");
        int id = getNumber(sc);

        sc.nextLine();

        System.out.print("Enter Device Name: ");
        String name = sc.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("Device name cannot be empty.");
            return;
        }

        Device device = DeviceFactory.createDevice(type, id, name);

        if (device != null) {
            manager.addDevice(device);
        }
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        DeviceManager manager = new DeviceManager();
        DeviceReport report = new DeviceReport();
        RoomManager roomManager = new RoomManager();

        DeviceRepository deviceRepository = new DeviceRepository();
        EnergyManager energyManager = new EnergyManager();
        TariffCalculator tariffCalculator = new TariffCalculator();
        EnergyReport energyReport = new EnergyReport(energyManager, tariffCalculator);
        BackupManager backupManager = new BackupManager();
        ActivityLog activityLog = ActivityLog.getInstance();

        SensorManager sensorManager = new SensorManager();
        SensorRepository sensorRepository = new SensorRepository();
        RuleManager ruleManager = new RuleManager();
        AutomationEngine automationEngine = new AutomationEngine(manager);

        // Load saved devices; on the very first run use the sample devices.
        if (deviceRepository.restoreInto(manager) == 0) {
            manager.addDevice(new Light(1, "Living Room Light"));
            manager.addDevice(new AC(2, "Bedroom AC"));
            manager.addDevice(new Fan(3, "Ceiling Fan"));
        }
        sensorRepository.restoreInto(sensorManager);
        energyManager.loadFromFile();
        backupManager.startAutoBackup(5 * 60 * 1000);   // background thread: backup every 5 minutes

        int choice;

        do {
            System.out.println("\n===== HOMESYNC SMART HOME =====");
            System.out.println("1. View All Devices");
            System.out.println("2. Turn Device ON");
            System.out.println("3. Turn Device OFF");
            System.out.println("4. Search Device");
            System.out.println("5. Remove Device");
            System.out.println("6. Device Statistics");
            System.out.println("7. Generate Device Report");
            System.out.println("8. Add New Device");
            System.out.println("9. Add Room");
            System.out.println("10. Assign Device to Room");
            System.out.println("11. View Rooms");
            System.out.println("12. Record Energy Usage");
            System.out.println("13. Energy Report");
            System.out.println("14. Set Electricity Tariff");
            System.out.println("15. Start Live Energy Monitor (thread)");
            System.out.println("16. Stop Live Energy Monitor");
            System.out.println("17. Create Backup");
            System.out.println("18. Restore Latest Backup");
            System.out.println("19. View Activity Log");
            System.out.println("20. Thread Demo (race condition)");
            System.out.println("21. Save All Data");
            System.out.println("22. Add Sensor");
            System.out.println("23. Update Sensor Reading (simulated)");
            System.out.println("24. View Sensors");
            System.out.println("25. Add Automation Rule");
            System.out.println("26. View Automation Rules");
            System.out.println("27. Run Automation");
            System.out.println("0. Exit");

            System.out.print("Enter your choice: ");
            choice = getNumber(sc);

            switch (choice) {

                case 1:
                    manager.displayDevices();
                    break;

                case 2:
                    System.out.print("Enter Device ID: ");
                    int onId = getNumber(sc);
                    Device onDevice = manager.searchDevice(onId);
                    manager.turnDeviceOn(onId);
                    if (onDevice != null) {
                        activityLog.logDeviceOn(onId, onDevice.getDeviceName());
                    }
                    break;

                case 3:
                    System.out.print("Enter Device ID: ");
                    int offId = getNumber(sc);
                    Device offDevice = manager.searchDevice(offId);
                    manager.turnDeviceOff(offId);
                    if (offDevice != null) {
                        activityLog.logDeviceOff(offId, offDevice.getDeviceName());
                    }
                    break;

                case 4:
                    System.out.print("Enter Device ID: ");
                    int searchId = getNumber(sc);

                    Device found = manager.searchDevice(searchId);

                    if (found != null) {
                        found.displayDetails();
                    } else {
                        System.out.println("Device not found.");
                    }
                    break;

                case 5:
                    System.out.print("Enter Device ID: ");
                    int removeId = getNumber(sc);
                    boolean existed = manager.searchDevice(removeId) != null;
                    manager.removeDevice(removeId);
                    roomManager.removeDeviceFromRooms(removeId);
                    if (existed) {
                        activityLog.logDeviceRemoved(removeId);
                    }
                    break;

                case 6:
                    manager.displayStatistics();
                    break;

                case 7:
                    report.generateReport(manager.getDevices());
                    break;

                case 8:
                    int countBefore = manager.getDevices().size();
                    addNewDevice(sc, manager);
                    if (manager.getDevices().size() > countBefore) {
                        Device added = manager.getDevices().get(manager.getDevices().size() - 1);
                        activityLog.logDeviceAdded(added.getDeviceId(), added.getDeviceName());
                    }
                    break;

                case 9:
                    sc.nextLine();
                    System.out.print("Enter Room Name: ");
                    String roomName = sc.nextLine();
                    roomManager.addRoom(roomName);
                    break;

                case 10:
                    System.out.print("Enter Device ID: ");
                    int deviceId = getNumber(sc);

                    Device device = manager.searchDevice(deviceId);

                    if (device == null) {
                        System.out.println("Device not found.");
                    } else {
                        sc.nextLine();
                        System.out.print("Enter Room Name: ");
                        String assignedRoom = sc.nextLine();
                        roomManager.assignDeviceToRoom(device, assignedRoom);
                    }
                    break;

                case 11:
                    roomManager.displayRooms();
                    break;

                case 12:
                    System.out.print("Enter Device ID: ");
                    Device usedDevice = manager.searchDevice(getNumber(sc));
                    if (usedDevice == null) {
                        System.out.println("Device not found.");
                    } else {
                        System.out.print("Enter usage in hours: ");
                        double hours = getDecimal(sc);
                        try {
                            double watts = energyManager.getPowerRating(usedDevice);
                            energyManager.recordUsage(usedDevice, hours);
                            tariffCalculator.displayCalculation(EnergyRecord.calculateKwh(watts, hours));
                        } catch (IllegalArgumentException e) {
                            System.out.println("Could not record usage: " + e.getMessage());
                        }
                    }
                    break;

                case 13:
                    energyReport.displayReport();
                    break;

                case 14:
                    System.out.print("Enter tariff per kWh (Rs.): ");
                    double newTariff = getDecimal(sc);
                    try {
                        tariffCalculator.setTariffPerKwh(newTariff);
                        System.out.println("Tariff set to Rs. " + newTariff + " per kWh.");
                    } catch (IllegalArgumentException e) {
                        System.out.println(e.getMessage());
                    }
                    break;

                case 15:
                    if (energyManager.startMonitoring(manager)) {
                        System.out.println("Energy monitor started (thread state: "
                                + energyManager.getMonitorState() + ").");
                        System.out.println("It records usage of every ON device every "
                                + EnergyManager.DEFAULT_INTERVAL_MILLIS / 1000 + " seconds.");
                    } else {
                        System.out.println("Energy monitor is already running.");
                    }
                    break;

                case 16:
                    if (energyManager.stopMonitoring()) {
                        System.out.println("Energy monitor stopped after "
                                + energyManager.getMonitorTicks() + " tick(s).");
                    } else {
                        System.out.println("Energy monitor is not running.");
                    }
                    break;

                case 17:
                    String backupName = backupManager.createBackup();
                    System.out.println(backupName == null
                            ? "Nothing to back up yet. Use option 21 to save data first."
                            : "Backup created: " + backupName);
                    break;

                case 18:
                    if (backupManager.restoreLatestBackup()) {
                        manager.getDevices().clear();
                        deviceRepository.restoreInto(manager);
                        for (Sensor oldSensor : new ArrayList<>(sensorManager.getSensors())) {
                            sensorManager.removeSensor(oldSensor.getSensorId());
                        }
                        sensorRepository.restoreInto(sensorManager);
                        energyManager.loadFromFile();
                        System.out.println("Devices, sensors and energy records reloaded from the backup.");
                    }
                    break;

                case 19:
                    activityLog.displayRecent(10);
                    break;

                case 20:
                    energyManager.demonstrateRaceCondition();
                    break;

                case 21:
                    saveAllData(manager, deviceRepository, energyManager, sensorManager, sensorRepository);
                    break;

                case 22:
                    addNewSensor(sc, sensorManager, activityLog);
                    break;

                case 23:
                    System.out.print("Enter Sensor ID: ");
                    int sensorId = getNumber(sc);
                    Sensor updatedSensor = sensorManager.searchSensor(sensorId);
                    sensorManager.updateSensorReading(sensorId);
                    if (updatedSensor != null && updatedSensor.isActive()) {
                        activityLog.logSensorUpdated(updatedSensor.getSensorName(), updatedSensor.getCurrentReading());
                    }
                    break;

                case 24:
                    sensorManager.displaySensors();
                    break;

                case 25:
                    addAutomationRule(sc, manager, ruleManager, automationEngine, activityLog);
                    break;

                case 26:
                    ruleManager.displayRules();
                    break;

                case 27:
                    runAutomation(sc, manager, sensorManager, automationEngine, activityLog);
                    break;

                case 0:
                    energyManager.stopMonitoring();
                    backupManager.stopAutoBackup();
                    saveAllData(manager, deviceRepository, energyManager, sensorManager, sensorRepository);
                    System.out.println("Thank you for using HOMESYNC!");
                    break;

                default:
                    System.out.println("Invalid choice. Try again.");
            }

        } while (choice != 0);

        sc.close();
    }
}
