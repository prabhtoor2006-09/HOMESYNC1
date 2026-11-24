
package com.homesync.model;

import com.homesync.service.DeviceManager;

public class BrightnessRule extends AutomationRule {

    private int lightDeviceId;
    private double threshold;
    private boolean lightShouldBeOn = false;

    public BrightnessRule(int ruleId, String ruleName, int lightDeviceId) {
        super(ruleId, ruleName);
        this.lightDeviceId = lightDeviceId;
        this.threshold = 30.0;
    }

    @Override
    public boolean evaluate(double temperature, double brightness, boolean motionDetected) {

        if (Double.isNaN(brightness) || Double.isInfinite(brightness)) {
            return false;
        }

        lightShouldBeOn = brightness < threshold;
        return true;
    }

    @Override
    public void execute(DeviceManager deviceManager) {

        Device light = deviceManager.searchDevice(lightDeviceId);

        if (light == null) {
            System.out.println("Light device not found.");
            return;
        }

        if (lightShouldBeOn && !light.isOn()) {
            light.turnOn();
        } else if (!lightShouldBeOn && light.isOn()) {
            light.turnOff();
        }
    }
}
