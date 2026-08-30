package com.nexusuniverse.survival.api;

import com.nexusuniverse.survival.config.NexusSurvivalConfig;
import com.nexusuniverse.survival.data.PlayerDataManager;
import com.nexusuniverse.survival.data.SurvivalPlayerData;
import com.nexusuniverse.survival.disease.Disease;
import com.nexusuniverse.survival.disease.DiseaseItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Public read-only API for a player's current survival stats. Registered
 * with Bukkit's ServicesManager on enable (see NexusSurvivalPlugin), the
 * same pattern NexusRealms uses for NexusRealmsApi - a consuming plugin
 * (NexusDreams first, more likely later) looks this up via reflection +
 * Bukkit.getServicesManager().getRegistration(), exactly like
 * NexusServerRules' integration/LandTrustBridge.java does for NexusRealms.
 * NexusSurvival never needs to know who's consuming this, and keeps
 * working completely standalone if nothing downstream is installed.
 *
 * Every fraction is normalized to 0.0 (worst) - 1.0 (best/full), so a
 * caller doesn't need to know this plugin's internal scales (thirst is
 * 0-180, rad-oxygen is 0-20, dirtiness is 0-100 and inverted) to make
 * sense of the numbers. Returns the "everything's fine" value (1.0 for
 * fractions, false/0 for infection) for a player who isn't currently
 * online rather than throwing - SurvivalPlayerData only exists for
 * players PlayerDataManager has actually seen this session, and a
 * reflective caller should never get an exception out of this class.
 */
public final class NexusSurvivalApi {

    private static final double MAX_RAD_OXYGEN = 20.0;
    private static final double MAX_DIRTINESS = 100.0;

    private final PlayerDataManager playerDataManager;
    private final NexusSurvivalConfig config;
    private final DiseaseItems diseaseItems;

    public NexusSurvivalApi(PlayerDataManager playerDataManager, NexusSurvivalConfig config, DiseaseItems diseaseItems) {
        this.playerDataManager = playerDataManager;
        this.config = config;
        this.diseaseItems = diseaseItems;
    }

    /** 1.0 = fully hydrated, 0.0 = empty/dehydrated. Reads thirst.max fresh from config every call, so a reload-time change to the bar's length is reflected immediately, not just a stale constant from whenever this plugin started. */
    public double thirstFraction(UUID playerId) {
        SurvivalPlayerData data = dataOrNull(playerId);
        return data == null ? 1.0 : clamp(data.thirst / config.thirstMax());
    }

    /** 1.0 = full rad-oxygen (safe), 0.0 = suffocating on radiation. */
    public double radOxygenFraction(UUID playerId) {
        SurvivalPlayerData data = dataOrNull(playerId);
        return data == null ? 1.0 : clamp(data.radOxygen / MAX_RAD_OXYGEN);
    }

    /** 1.0 = spotless, 0.0 = filthy. Inverted from the internal 0(clean)-100(filthy) scale. */
    public double hygieneFraction(UUID playerId) {
        SurvivalPlayerData data = dataOrNull(playerId);
        return data == null ? 1.0 : clamp(1.0 - (data.dirtiness / MAX_DIRTINESS));
    }

    public boolean isInfected(UUID playerId) {
        SurvivalPlayerData data = dataOrNull(playerId);
        return data != null && data.infection != null;
    }

    /** 0 (Mild) - 3 (Critical). 0 if not currently infected. */
    public int infectionSeverity(UUID playerId) {
        SurvivalPlayerData data = dataOrNull(playerId);
        return (data == null || data.infection == null) ? 0 : data.infectionSeverity;
    }

    // --- Disease catalog: lets a plugin like NexusEconomy sell "Pathogen Vial" items for every
    //     disease this plugin defines, without knowing anything about Disease as a Java type.
    //     Same reflection-catalog shape as NexusVice's NexusViceAPI (allSubstanceIds/
    //     createSubstanceItem etc.) -- ids are just Disease#name(), stable across restarts. ---

    /** Every disease id (Disease#name()), in declared order. */
    public List<String> allDiseaseIds() {
        List<String> ids = new ArrayList<>();
        for (Disease disease : Disease.values()) {
            ids.add(disease.name());
        }
        return ids;
    }

    public String diseaseDisplayName(String diseaseId) {
        Disease disease = diseaseOrNull(diseaseId);
        return disease == null ? null : disease.getDisplayName();
    }

    public String diseaseDescription(String diseaseId) {
        Disease disease = diseaseOrNull(diseaseId);
        return disease == null ? null : disease.getDescription();
    }

    /** True for diseases whose symptoms include Wither -- the ones that can actually kill on their own at Critical severity, the same cases NexusSurvival's own severity system treats as the nastiest. */
    public boolean diseaseIsSevere(String diseaseId) {
        Disease disease = diseaseOrNull(diseaseId);
        if (disease == null) return false;
        for (PotionEffect effect : disease.getSymptoms()) {
            if (effect.getType().equals(PotionEffectType.WITHER)) return true;
        }
        return false;
    }

    /**
     * A real, usable Pathogen Vial for this disease -- drinking it infects the player, exactly
     * like /nexussurvival give would, just sold through another plugin's shop. Null for an
     * unrecognized id.
     */
    public ItemStack createDiseaseItem(String diseaseId) {
        Disease disease = diseaseOrNull(diseaseId);
        return disease == null ? null : diseaseItems.createInfector(disease);
    }

    private Disease diseaseOrNull(String diseaseId) {
        if (diseaseId == null) return null;
        try {
            return Disease.valueOf(diseaseId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private SurvivalPlayerData dataOrNull(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return null;
        return playerDataManager.get(player);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
