
package com.homesync.model;

import com.homesync.service.DeviceManager;

public class TemperatureRule extends AutomationRule {

    private int acDeviceId;
    private double upperThreshold;
    private double lowerThreshold;
    private Boolean coolingRequired = null;

    public TemperatureRule(int ruleId, String ruleName, int acDeviceId) {
        super(ruleId, ruleName);
        this.acDeviceId = acDeviceId;
        this.upperThreshold = 28.0;
        this.lowerThreshold = 24.0;
    }

    @Override
    public boolean evaluate(double temperature, double brightness, boolean motionDetected) {

        if (Double.isNaN(temperature) || Double.isInfinite(temperature)) {
            return false;
        }

        if (temperature > upperThreshold) {
            coolingRequired = true;
        } else if (temperature < lowerThreshold) {
            coolingRequired = false;
        } else {
            return false;
        }

        return true;
    }

    @Override
    public void execute(DeviceManager deviceManager) {

        Device ac = deviceManager.searchDevice(acDeviceId);

        if (ac == null) {
            System.out.println("AC device not found.");
            return;
        }

        if (coolingRequired == null) {
            return;
        }

        if (coolingRequired && !ac.isOn()) {
            ac.turnOn();
        } else if (!coolingRequired && ac.isOn()) {
            ac.turnOff();
        }
    }
}
