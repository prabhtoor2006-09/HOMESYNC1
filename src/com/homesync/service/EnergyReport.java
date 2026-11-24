package com.homesync.service;

import com.homesync.model.EnergyRecord;
import com.homesync.utils.TariffCalculator;
import java.util.List;
import java.util.Map;

/**
 * EnergyReport - builds and prints the energy report.
 * It only READS data from EnergyManager and uses TariffCalculator for cost.
 */
public class EnergyReport {

    private final EnergyManager energyManager;
    private final TariffCalculator tariffCalculator;

    public EnergyReport(EnergyManager energyManager, TariffCalculator tariffCalculator) {
        if (energyManager == null || tariffCalculator == null) {
            throw new IllegalArgumentException("EnergyManager and TariffCalculator are required.");
        }
        this.energyManager = energyManager;
        this.tariffCalculator = tariffCalculator;
    }

    /** Full report as text (StringBuilder is faster than joining Strings with +). */
    public String buildReport() {
        List<EnergyRecord> records = energyManager.getRecords();
        Map<Integer, Double> deviceEnergy = energyManager.getDeviceEnergy();
        double total = energyManager.getTotalEnergy();

        StringBuilder report = new StringBuilder();
        report.append("\n===== HOMESYNC ENERGY REPORT =====\n");

        if (records.isEmpty()) {
            report.append("No energy records available.\n");
            return report.toString();
        }

        report.append("\n--- Device-wise consumption ---\n");
        for (Map.Entry<Integer, Double> entry : deviceEnergy.entrySet()) {
            double kwh = entry.getValue();
            double share = (total > 0) ? (kwh / total) * 100 : 0;
            report.append(String.format("Device %d (%s): %.3f kWh (%.1f%%) - cost %s %.2f%n",
                    entry.getKey(), energyManager.getDeviceName(entry.getKey()), kwh, share,
                    TariffCalculator.CURRENCY, tariffCalculator.calculateCost(kwh)));
        }

        report.append("\n--- Individual records ---\n");
        for (EnergyRecord record : records) {
            report.append(record).append("\n");
        }

        report.append("\n--- Summary ---\n");
        report.append(String.format("Total records      : %d%n", records.size()));
        report.append(String.format("Total energy       : %.3f kWh%n", total));
        report.append(String.format("Tariff             : %s %.2f per kWh%n",
                TariffCalculator.CURRENCY, tariffCalculator.getTariffPerKwh()));
        report.append(String.format("Estimated cost     : %s %.2f%n",
                TariffCalculator.CURRENCY, tariffCalculator.calculateCost(total)));

        int top = energyManager.getTopConsumerId();
        if (top != -1) {
            report.append(String.format("Highest consumer   : %s (%.3f kWh)%n",
                    energyManager.getDeviceName(top), energyManager.getEnergyForDevice(top)));
        }
        report.append("(Simulated usage - not real-time electricity monitoring)\n");
        return report.toString();
    }

    /** Prints the full report and logs the event. */
    public void displayReport() {
        System.out.println(buildReport());
        ActivityLog.getInstance().logEnergyReportGenerated();
    }

    /** Overloaded: report for one device only. */
    public void displayReport(int deviceId) {
        List<EnergyRecord> records = energyManager.getRecordsForDevice(deviceId);

        System.out.println("\n===== ENERGY REPORT: " + energyManager.getDeviceName(deviceId) + " =====");
        if (records.isEmpty()) {
            System.out.println("No energy records for device " + deviceId + ".");
            return;
        }
        for (EnergyRecord record : records) {
            System.out.println(record);
        }
        double kwh = energyManager.getEnergyForDevice(deviceId);
        System.out.println(String.format("Total: %.3f kWh | Estimated cost: %s %.2f",
                kwh, TariffCalculator.CURRENCY, tariffCalculator.calculateCost(kwh)));
        ActivityLog.getInstance().logEnergyReportGenerated();
    }
}
