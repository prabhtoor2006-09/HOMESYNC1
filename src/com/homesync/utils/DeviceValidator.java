package com.homesync.utils;

import java.util.ArrayList;
import com.homesync.model.Device;

public class DeviceValidator {

    public static boolean isValidId(int id) {
        return id > 0;
    }

    public static boolean isUniqueId(int id, ArrayList<Device> devices) {
        for (Device device : devices) {
            if (device.getDeviceId() == id) {
                return false;
            }
        }

        return true;
    }
}
