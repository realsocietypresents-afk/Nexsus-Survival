package com.nexusuniverse.vice;

import com.nexusuniverse.vice.substances.Substance;

import java.util.EnumMap;
import java.util.Map;

public class VicePlayerData {

    private final Map<Substance, Double> substanceDoses = new EnumMap<>(Substance.class);
    private double alcoholDose = 0.0;
    private long lastVomitTick = Long.MIN_VALUE;
    private long lastRehabTick = Long.MIN_VALUE;
    private boolean inBlackout = false;
    private final Map<Substance, Double> peakDoseThisEpisode = new EnumMap<>(Substance.class); // tracks the high point, used to decide if a "crash" is owed once it decays back down
    private double peakAlcoholThisEpisode = 0.0;

    public double substanceDose(Substance substance) {
        return substanceDoses.getOrDefault(substance, 0.0);
    }

    public void addSubstanceDose(Substance substance, double amount) {
        double newDose = substanceDose(substance) + amount;
        substanceDoses.put(substance, newDose);
        peakDoseThisEpisode.put(substance, Math.max(peakDoseThisEpisode.getOrDefault(substance, 0.0), newDose));
    }

    public void setSubstanceDose(Substance substance, double amount) {
        substanceDoses.put(substance, Math.max(0, amount));
    }

    public double peakSubstanceDose(Substance substance) {
        return peakDoseThisEpisode.getOrDefault(substance, 0.0);
    }

    public void resetPeak(Substance substance) {
        peakDoseThisEpisode.put(substance, 0.0);
    }

    public double alcoholDose() {
        return alcoholDose;
    }

    public void addAlcoholDose(double amount) {
        alcoholDose += amount;
        peakAlcoholThisEpisode = Math.max(peakAlcoholThisEpisode, alcoholDose);
    }

    public void setAlcoholDose(double amount) {
        alcoholDose = Math.max(0, amount);
    }

    public double peakAlcoholDose() {
        return peakAlcoholThisEpisode;
    }

    public void resetAlcoholPeak() {
        peakAlcoholThisEpisode = 0;
    }

    public long lastVomitTick() {
        return lastVomitTick;
    }

    public void setLastVomitTick(long tick) {
        this.lastVomitTick = tick;
    }

    public long lastRehabTick() {
        return lastRehabTick;
    }

    public void setLastRehabTick(long tick) {
        this.lastRehabTick = tick;
    }

    public boolean isInBlackout() {
        return inBlackout;
    }

    public void setInBlackout(boolean inBlackout) {
        this.inBlackout = inBlackout;
    }

    /** Wipes every active dose -- used by rehab. Peaks/history are left alone (rehab clears the high, not the record). */
    public void clearAllDoses() {
        substanceDoses.clear();
        alcoholDose = 0.0;
        inBlackout = false;
    }
}
