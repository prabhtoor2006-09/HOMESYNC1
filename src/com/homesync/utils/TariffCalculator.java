package com.homesync.utils;

import com.homesync.model.EnergyRecord;

/**
 * TariffCalculator - turns energy (kWh) into an estimated electricity cost.
 *
 * Formula used:  Cost = kWh x tariff per kWh
 *
 * The tariff is NOT fixed: it is passed in the constructor or changed with
 * setTariffPerKwh(). DEFAULT_TARIFF_PER_KWH is only the starting value
 * (the sample rate from the project brief: Rs. 8 per unit).
 */
public class TariffCalculator {

    public static final double DEFAULT_TARIFF_PER_KWH = 8.0;
    public static final String CURRENCY = "Rs.";

    private double tariffPerKwh;

    public TariffCalculator() {
        this(DEFAULT_TARIFF_PER_KWH);
    }

    public TariffCalculator(double tariffPerKwh) {
        setTariffPerKwh(tariffPerKwh);
    }

    public double getTariffPerKwh() {
        return tariffPerKwh;
    }

    public void setTariffPerKwh(double tariffPerKwh) {
        if (tariffPerKwh < 0) {
            throw new IllegalArgumentException("Tariff cannot be negative.");
        }
        this.tariffPerKwh = tariffPerKwh;
    }

    /** Cost for a given number of kWh. */
    public double calculateCost(double energyKwh) {
        if (energyKwh < 0) {
            throw new IllegalArgumentException("Energy cannot be negative.");
        }
        return energyKwh * tariffPerKwh;
    }

    /** Overloaded: cost directly from power (watts) and usage time (hours). */
    public double calculateCost(double powerWatts, double hoursUsed) {
        return calculateCost(EnergyRecord.calculateKwh(powerWatts, hoursUsed));
    }

    /** Prints the calculation step by step so it is easy to explain. */
    public void displayCalculation(double energyKwh) {
        double cost = calculateCost(energyKwh);
        System.out.println("--- Tariff Calculation ---");
        System.out.println("Energy consumed : " + String.format("%.3f", energyKwh) + " kWh");
        System.out.println("Tariff          : " + CURRENCY + " " + String.format("%.2f", tariffPerKwh) + " per kWh");
        System.out.println("Cost = " + String.format("%.3f", energyKwh) + " x "
                + String.format("%.2f", tariffPerKwh) + " = " + CURRENCY + " " + String.format("%.2f", cost));
    }
}
