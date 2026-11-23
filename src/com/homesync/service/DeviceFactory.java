
package com.homesync.service;

import com.homesync.model.Device;
import com.homesync.model.Light;
import com.homesync.model.AC;
import com.homesync.model.Fan;

public class DeviceFactory {

    public static Device createDevice(int type, int id, String name) {

        switch (type) {
            case 1:
                return new Light(id, name);

            case 2:
                return new AC(id, name);

            case 3:
                return new Fan(id, name);

            default:
                return null;
        }
    }
}
