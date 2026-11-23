
package com.homesync.service;

import java.util.ArrayList;
import com.homesync.model.Device;

public class DeviceReport {

    public void generateReport(ArrayList<Device> devices) {

        int active = 0;
        int inactive = 0;

        System.out.println("\n===== HOMESYNC DEVICE REPORT =====");

        if (devices.isEmpty()) {
            System.out.println("No devices registered.");
            return;
        }

        for (Device device : devices) {

            device.displayDetails();

            if (device.isOn()) {
                active++;
            } else {
                inactive++;
            }

            System.out.println("----------------------");
        }

        System.out.println("Total Devices: " + devices.size());
        System.out.println("Active Devices: " + active);
        System.out.println("Inactive Devices: " + inactive);
    }
}
