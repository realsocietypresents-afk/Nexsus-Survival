package com.nexusuniverse.vice.alcohol;

import org.bukkit.Color;

/**
 * A specific drinkable product. Every brand ties back to one of the three base AlcoholType
 * profiles (BEER/WINE/LIQUOR) -- they all still feed the SAME shared alcohol dose pool and run
 * through the SAME effect curve in ViceEffectManager (there's no per-type effect difference, only
 * per-dose-ratio). A brand only changes two things: how strong one drink of it is (its quality
 * tier's dose multiplier against the base type's dose-per-item, see
 * ViceConfig#dosePerItem(AlcoholBrand)) and how it's named/colored/crafted.
 *
 * Three brands per type, one per quality tier. Adding a fourth of any type, or a fourth type
 * entirely, is just more entries here plus a recipe in ViceRecipes -- nothing else changes.
 */
public enum AlcoholBrand {

    RUSTBUCKET_LAGER(AlcoholType.BEER, AlcoholQuality.BOTTOM_SHELF, Color.fromRGB(196, 156, 60)),
    HOPFIELD_ALE(AlcoholType.BEER, AlcoholQuality.STANDARD, Color.fromRGB(212, 155, 45)),
    IRONCREST_STOUT(AlcoholType.BEER, AlcoholQuality.TOP_SHELF, Color.fromRGB(90, 55, 25)),

    CELLAR_JUG_RED(AlcoholType.WINE, AlcoholQuality.BOTTOM_SHELF, Color.fromRGB(140, 30, 55)),
    HIGHVALE_MERLOT(AlcoholType.WINE, AlcoholQuality.STANDARD, Color.fromRGB(115, 20, 45)),
    ESTATE_RESERVE(AlcoholType.WINE, AlcoholQuality.TOP_SHELF, Color.fromRGB(80, 10, 35)),

    MOONSHINE_JAR(AlcoholType.LIQUOR, AlcoholQuality.BOTTOM_SHELF, Color.fromRGB(230, 230, 200)),
    BLACKROCK_WHISKEY(AlcoholType.LIQUOR, AlcoholQuality.STANDARD, Color.fromRGB(180, 110, 30)),
    GILDED_RESERVE(AlcoholType.LIQUOR, AlcoholQuality.TOP_SHELF, Color.fromRGB(220, 175, 60));

    private final AlcoholType type;
    private final AlcoholQuality quality;
    private final Color defaultTint;

    AlcoholBrand(AlcoholType type, AlcoholQuality quality, Color defaultTint) {
        this.type = type;
        this.quality = quality;
        this.defaultTint = defaultTint;
    }

    public AlcoholType type() {
        return type;
    }

    public AlcoholQuality quality() {
        return quality;
    }

    public Color defaultTint() {
        return defaultTint;
    }

    public String configKey() {
        return name().toLowerCase();
    }
}
