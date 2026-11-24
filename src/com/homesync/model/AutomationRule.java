
package com.homesync.model;

import com.homesync.service.DeviceManager;

public abstract class AutomationRule {

    private int ruleId;
    private String ruleName;
    private boolean enabled;

    public AutomationRule(int ruleId, String ruleName) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.enabled = true;
    }

    public int getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public abstract boolean evaluate(
        double temperature,
        double brightness,
        boolean motionDetected
    );

    public abstract void execute(DeviceManager deviceManager);
}

