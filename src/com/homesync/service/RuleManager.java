
package com.homesync.service;

import java.util.ArrayList;
import java.util.HashMap;
import  java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.homesync.model.AutomationRule;

public class RuleManager {

    private HashMap<Integer, AutomationRule> rules = new HashMap<>();
    private HashSet<Integer> ruleIds = new HashSet<>();

    public void addRule(AutomationRule rule) {

        if (rule == null) {
            System.out.println("Invalid rule.");
            return;
        }

        int id = rule.getRuleId();

        if (ruleIds.contains(id)) {
            System.out.println("Rule ID already exists.");
            return;
        }

        rules.put(id, rule);
        ruleIds.add(id);

        System.out.println("Rule added successfully.");
    }

    public AutomationRule searchRule(int id) {
        return rules.get(id);
    }

    public void removeRule(int id) {

        if (rules.containsKey(id)) {
            rules.remove(id);
            ruleIds.remove(id);
            System.out.println("Rule removed successfully.");
        } else {
            System.out.println("Rule not found.");
        }
    }

    public void enableRule(int id) {

        AutomationRule rule = searchRule(id);

        if (rule != null) {
            rule.setEnabled(true);
            System.out.println("Rule enabled.");
        } else {
            System.out.println("Rule not found.");
        }
    }

    public void disableRule(int id) {

        AutomationRule rule = searchRule(id);

        if (rule != null) {
            rule.setEnabled(false);
            System.out.println("Rule disabled.");
        } else {
            System.out.println("Rule not found.");
        }
    }

    public void displayRules() {

        if (rules.isEmpty()) {
            System.out.println("No automation rules available.");
            return;
        }

        for (AutomationRule rule : rules.values()) {
            System.out.println("Rule ID: " + rule.getRuleId());
            System.out.println("Rule Name: " + rule.getRuleName());
            System.out.println("Status: " +
                (rule.isEnabled() ? "Enabled" : "Disabled"));
            System.out.println("----------------------");
        }
    }

    public ArrayList<AutomationRule> getActiveRules() {

        ArrayList<AutomationRule> activeRules = new ArrayList<>();

        for (AutomationRule rule : rules.values()) {
            if (rule.isEnabled()) {
                activeRules.add(rule);
            }
        }

        return activeRules;
    }
}
