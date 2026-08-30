package com.nexusuniverse.vice.alcohol;

/** Beer/wine/liquor -- alcohol is its own separate dose track from the substances, with its own decay and blackout threshold. */
public enum AlcoholType {

    BEER(8),
    WINE(18),
    LIQUOR(35);

    private final double defaultDosePerItem;

    AlcoholType(double defaultDosePerItem) {
        this.defaultDosePerItem = defaultDosePerItem;
    }

    public double defaultDosePerItem() {
        return defaultDosePerItem;
    }

    public String configKey() {
        return name().toLowerCase();
    }
}
