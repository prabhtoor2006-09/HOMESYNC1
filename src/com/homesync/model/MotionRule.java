
package com.homesync.model;

import com.homesync.service.DeviceManager;

public class MotionRule extends AutomationRule {

    private int lightDeviceId;
    private boolean motionDetected;

    public MotionRule(int ruleId, String ruleName, int lightDeviceId) {
        super(ruleId, ruleName);
        this.lightDeviceId = lightDeviceId;
    }

    @Override
    public boolean evaluate(double temperature, double brightness, boolean motionDetected) {
        this.motionDetected = motionDetected;
        return motionDetected;
    }

    @Override
    public void execute(DeviceManager deviceManager) {

        if (!motionDetected) {
            return;
        }

        Device light = deviceManager.searchDevice(lightDeviceId);

        if (light == null) {
            System.out.println("Motion sensor light not found.");
            return;
        }

        if (!light.isOn()) {
            light.turnOn();
            System.out.println("Motion detected! Light turned ON.");
        }
    }
}
