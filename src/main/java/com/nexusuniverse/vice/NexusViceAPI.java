package com.nexusuniverse.vice;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Public read-only surface for other plugins to use without a hard compile-time dependency on
 * this plugin. Registered via Bukkit's ServicesManager on enable, same pattern as
 * NexusSeasonsAPI/NexusEnchantsAPI. A plugin with no compile-time dependency on NexusVice looks
 * this up reflectively against these exact method signatures -- see NexusLegends' ViceBridge
 * (lifetime-stats side) and NexusEconomy's own NexusViceBridge (shop-catalog side, added
 * alongside the methods below) for that pattern.
 */
public interface NexusViceAPI {

    /** How many times this player has crossed into an active overdose/blackout state, ever (persisted, survives restarts). */
    int getOverdoseCount(UUID playerId);

    // --- Shop catalog surface, for NexusEconomy's "Vice" shop tab ---

    /** Every substance's internal id (Substance#configKey(), e.g. "fentinoli") -- stable across restarts, safe to use as a shop/config key. */
    List<String> allSubstanceIds();

    /** The player-facing name for a substance id, as configured (falls back to a title-cased id if unrecognized). */
    String substanceDisplayName(String id);

    /** e.g. "DEPRESSANT", "STIMULANT", "EUPHORIC" -- an opaque grouping key for pricing/sorting, not a Bukkit type. Null if the id isn't recognized. */
    String substanceCategoryName(String id);

    /** True if reaching this substance's overdose threshold triggers real blackout damage (see NexusMorality's Problem+Solution wiring for that damage). False (never true) for an unrecognized id. */
    boolean substanceHasOverdoseRisk(String id);

    /** This substance's configured overdose threshold -- a rough potency signal (lower threshold = a little goes further). 0 if the id isn't recognized. */
    double substanceOverdoseThreshold(String id);

    /** A real, usable copy of this substance's item -- identical to what /vice give hands out. Null if the id isn't recognized. */
    ItemStack createSubstanceItem(String id);

    /** Every alcohol brand's internal id (AlcoholBrand#configKey(), e.g. "hopfield_ale"). */
    List<String> allAlcoholBrandIds();

    String alcoholDisplayName(String id);

    /** e.g. "BEER", "WINE", "LIQUOR". Null if the id isn't recognized. */
    String alcoholTypeName(String id);

    /** e.g. "BOTTOM_SHELF", "STANDARD", "TOP_SHELF". Null if the id isn't recognized. */
    String alcoholQualityName(String id);

    /** A real, usable copy of this brand's item -- identical to what /vice give hands out. Null if the id isn't recognized. */
    ItemStack createAlcoholItem(String id);
}
