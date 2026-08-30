package com.nexusuniverse.vice.alcohol;

/**
 * How well-made a particular brand is. This scales that brand's base AlcoholType dose-per-item
 * up or down (see ViceConfig#dosePerItem(AlcoholBrand)) -- it isn't a separate mechanic, just a
 * multiplier on the one that already exists. Bottom-shelf stuff is weaker but presumably cheaper
 * to make/buy in bulk; top-shelf is a lot stronger per drink.
 */
public enum AlcoholQuality {

    BOTTOM_SHELF(0.75),
    STANDARD(1.0),
    TOP_SHELF(1.4);

    private final double doseMultiplier;

    AlcoholQuality(double doseMultiplier) {
        this.doseMultiplier = doseMultiplier;
    }

    public double doseMultiplier() {
        return doseMultiplier;
    }

    public String configKey() {
        return name().toLowerCase();
    }
}
