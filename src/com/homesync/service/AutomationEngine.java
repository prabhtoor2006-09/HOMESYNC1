
package com.homesync.service;

import java.util.ArrayList;
import com.homesync.model.AutomationRule;

public class AutomationEngine {

    private ArrayList<AutomationRule> rules = new ArrayList<>();
    private DeviceManager deviceManager;

    public AutomationEngine(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    public void addRule(AutomationRule rule) {
        if (rule != null) {
            rules.add(rule);
            System.out.println("Rule added: " + rule.getRuleName());
        }
    }

    public void evaluateRules(double temperature, double brightness, boolean motionDetected) {

        for (AutomationRule rule : rules) {

            if (!rule.isEnabled()) {
                continue;
            }

            boolean matched = rule.evaluate(
                temperature,
                brightness,
                motionDetected
            );

            if (matched) {
                rule.execute(deviceManager);
            }
        }
    }

    public void displayRules() {

        if (rules.isEmpty()) {
            System.out.println("No automation rules available.");
            return;
        }

        for (AutomationRule rule : rules) {
            System.out.println("Rule ID: " + rule.getRuleId());
            System.out.println("Rule Name: " + rule.getRuleName());
            System.out.println("Status: " + (rule.isEnabled() ? "Enabled" : "Disabled"));
            System.out.println("----------------------");
        }
    }
}
