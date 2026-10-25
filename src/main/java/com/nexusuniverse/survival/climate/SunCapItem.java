package com.nexusuniverse.survival.climate;

import com.nexusuniverse.survival.NexusSurvivalPlugin;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Worn in the helmet slot, same trick RadiationItems' Hazmat Mask already
 * uses (a dyed leather helmet, PDC-tagged so it's recognized regardless of
 * rename). Fully blocks summer heat-exhaustion progression while worn -- no
 * partial protection, same all-or-nothing design as the Hazmat Mask -- it
 * doesn't touch hypothermia/winter at all, this is specifically the "keep
 * the sun off you" answer to ClimateManager's summer path.
 *
 * No crafting recipe yet, same "give-command only for now" state as the
 * Hazmat Mask, wand, and summon items (see NexusSurvivalCommand#handleGive).
 */
public class SunCapItem {

    private final NamespacedKey sunCapKey;

    public SunCapItem(NexusSurvivalPlugin plugin) {
        this.sunCapKey = new NamespacedKey(plugin, "sun_cap");
    }

    public ItemStack createSunCap() {
        ItemStack item = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setDisplayName("§eWide-Brim Sun Cap");
        meta.setLore(java.util.List.of("§7Fully blocks heat exhaustion while worn."));
        meta.setColor(Color.fromRGB(222, 184, 90)); // straw/tan
        meta.getPersistentDataContainer().set(sunCapKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWearingCap(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || !helmet.hasItemMeta()) return false;
        Boolean tag = helmet.getItemMeta().getPersistentDataContainer().get(sunCapKey, PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(tag);
    }
}
