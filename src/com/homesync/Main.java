
package com.homesync;

import java.util.Scanner;

import com.homesync.model.Device;
import com.homesync.model.Light;
import com.homesync.model.AC;
import com.homesync.model.Fan;

import com.homesync.service.DeviceManager;
import com.homesync.service.DeviceReport;
import com.homesync.service.DeviceFactory;
import com.homesync.service.RoomManager;

public class Main {

    public static int getNumber(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.println("Invalid input! Please enter a number.");
            sc.next();
            System.out.print("Try again: ");
        }
        return sc.nextInt();
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

        manager.addDevice(new Light(1, "Living Room Light"));
        manager.addDevice(new AC(2, "Bedroom AC"));
        manager.addDevice(new Fan(3, "Ceiling Fan"));

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
            System.out.println("12. Exit");

            System.out.print("Enter your choice: ");
            choice = getNumber(sc);

            switch (choice) {

                case 1:
                    manager.displayDevices();
                    break;

                case 2:
                    System.out.print("Enter Device ID: ");
                    manager.turnDeviceOn(getNumber(sc));
                    break;

                case 3:
                    System.out.print("Enter Device ID: ");
                    manager.turnDeviceOff(getNumber(sc));
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
                    manager.removeDevice(removeId);
                    roomManager.removeDeviceFromRooms(removeId);
                    break;

                case 6:
                    manager.displayStatistics();
                    break;

                case 7:
                    report.generateReport(manager.getDevices());
                    break;

                case 8:
                    addNewDevice(sc, manager);
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
                    System.out.println("Thank you for using HOMESYNC!");
                    break;

                default:
                    System.out.println("Invalid choice. Try again.");
            }

        } while (choice != 12);

        sc.close();
    }
}
