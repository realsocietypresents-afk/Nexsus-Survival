package com.nexusuniverse.vice.alcohol;

import com.nexusuniverse.vice.ViceConfig;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;

import java.util.List;

public class AlcoholItems {

    private final ViceConfig config;
    private final NamespacedKey brandKey;

    public AlcoholItems(Plugin plugin, ViceConfig config) {
        this.config = config;
        this.brandKey = new NamespacedKey(plugin, "vice_alcohol_brand");
    }

    public ItemStack create(AlcoholBrand brand) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();

        meta.setBasePotionType(PotionType.WATER);
        meta.setDisplayName(qualityColor(brand.quality()) + config.displayName(brand));
        meta.setLore(List.of(
                "§7" + titleCase(brand.type().name()) + " -- " + qualityLabel(brand.quality()),
                "§7Right-click to drink.",
                "§8The more you have, the worse it gets."));
        meta.setColor(brand.defaultTint());
        meta.getPersistentDataContainer().set(brandKey, PersistentDataType.STRING, brand.name());

        item.setItemMeta(meta);
        return item;
    }

    /** Returns the brand this item represents, or null if it isn't one of ours. */
    public AlcoholBrand readBrand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(brandKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return AlcoholBrand.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String qualityColor(AlcoholQuality quality) {
        return switch (quality) {
            case BOTTOM_SHELF -> "§7";
            case STANDARD -> "§6";
            case TOP_SHELF -> "§e§l";
        };
    }

    private String qualityLabel(AlcoholQuality quality) {
        return switch (quality) {
            case BOTTOM_SHELF -> "Bottom-shelf";
            case STANDARD -> "Standard";
            case TOP_SHELF -> "Top-shelf";
        };
    }

    private String titleCase(String enumName) {
        String raw = enumName.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
