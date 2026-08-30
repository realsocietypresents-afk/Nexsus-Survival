package com.nexusuniverse.vice.substances;

import org.bukkit.Material;

/**
 * These are original, invented substances -- not stand-ins for any real
 * drug. The enum name is only ever used as an internal config/data key
 * (lowercased, e.g. "fentinoli"); every DISPLAY name is read from
 * config.yml at runtime, so renaming what players see is a one-line
 * config edit, never a code change.
 *
 * Each one is a real, distinct item now (sugar, snowballs, wheat, etc.)
 * rather than every substance sharing a generic potion-bottle look.
 *
 * The numbers here are DEFAULTS -- every one of them is overridable per
 * substance in config.yml under substances.<key>.*.
 */
public enum Substance {

    // category, dose added per item consumed, dose level that counts as "overdose", whether overdose has a real damage/blackout consequence, the physical item
    FENTINOLI(Category.DEPRESSANT, 40, 55, true, Material.SUGAR),
    XANAXEL(Category.DEPRESSANT, 30, 80, true, Material.PAPER),
    OPIATRIX(Category.DEPRESSANT, 35, 65, true, Material.POPPY),
    MOLOTINE(Category.STIMULANT, 30, 90, false, Material.BLAZE_POWDER),
    COCAINIUM(Category.STIMULANT, 35, 100, false, Material.SNOWBALL),
    MOLLYQ(Category.STIMULANT, 30, 100, false, Material.PINK_DYE),
    ACIDROP(Category.HALLUCINOGEN, 25, Integer.MAX_VALUE, false, Material.PHANTOM_MEMBRANE),
    HERBALIS(Category.MELLOW, 15, 200, false, Material.WHEAT),
    NICOTANE(Category.STIMULANT, 8, 300, false, Material.STICK),
    CAFFINEX(Category.STIMULANT, 6, 400, false, Material.COCOA_BEANS),
    CRYOTINE(Category.STIMULANT, 45, 70, true, Material.QUARTZ),
    SPORELINE(Category.HALLUCINOGEN, 25, Integer.MAX_VALUE, false, Material.RED_MUSHROOM),
    KETRAZINE(Category.DISSOCIATIVE, 35, 75, true, Material.PRISMARINE_CRYSTALS),
    ANABOLEX(Category.PERFORMANCE, 30, 120, false, Material.IRON_NUGGET),
    DRIFTWEED(Category.MELLOW, 15, 220, false, Material.KELP),
    SOMNARA(Category.DISSOCIATIVE, 35, 80, true, Material.ECHO_SHARD),
    TITANEX(Category.PERFORMANCE, 35, 130, false, Material.COPPER_INGOT),
    EUPHORION(Category.EUPHORIC, 25, 999999, false, Material.GLOW_BERRIES),
    BLISSENTA(Category.EUPHORIC, 20, 999999, false, Material.HONEYCOMB),
    RAPTURINE(Category.EUPHORIC, 35, 85, true, Material.AMETHYST_SHARD),

    // --- added this update: five more, rounding out categories that were thin ---
    MORPHENE(Category.DEPRESSANT, 38, 60, true, Material.GHAST_TEAR),
    CRANKSTONE(Category.STIMULANT, 40, 65, true, Material.NETHER_WART),
    RIFTSMOKE(Category.HALLUCINOGEN, 25, Integer.MAX_VALUE, false, Material.CRIMSON_FUNGUS),
    VOIDCAP(Category.DISSOCIATIVE, 38, 70, true, Material.CHORUS_FRUIT),
    GUMMEL(Category.MELLOW, 15, 210, false, Material.MELON_SLICE);

    private final Category category;
    private final double defaultDosePerItem;
    private final double defaultOverdoseThreshold;
    private final boolean defaultHasOverdoseRisk;
    private final Material material;

    Substance(Category category, double defaultDosePerItem, double defaultOverdoseThreshold, boolean defaultHasOverdoseRisk, Material material) {
        this.category = category;
        this.defaultDosePerItem = defaultDosePerItem;
        this.defaultOverdoseThreshold = defaultOverdoseThreshold;
        this.defaultHasOverdoseRisk = defaultHasOverdoseRisk;
        this.material = material;
    }

    public Category category() {
        return category;
    }

    public double defaultDosePerItem() {
        return defaultDosePerItem;
    }

    public double defaultOverdoseThreshold() {
        return defaultOverdoseThreshold;
    }

    public boolean defaultHasOverdoseRisk() {
        return defaultHasOverdoseRisk;
    }

    public Material material() {
        return material;
    }

    /** The config key this substance is looked up under, e.g. "fentinoli". */
    public String configKey() {
        return name().toLowerCase();
    }
}

