package com.nexusuniverse.survival.thirst;

import com.nexusuniverse.survival.NexusSurvivalPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

public class ThirstItems {

    private final NamespacedKey waterBottleKey;
    private final NamespacedKey rawWaterKey;

    public ThirstItems(NexusSurvivalPlugin plugin) {
        this.waterBottleKey = new NamespacedKey(plugin, "water_bottle");
        this.rawWaterKey = new NamespacedKey(plugin, "raw_water");
    }

    public ItemStack createWaterBottle() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(PotionType.WATER);
        applyWaterBottleLook(meta);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createRawWater() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(PotionType.WATER);
        applyRawWaterLook(meta);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Upgrades a plain, freshly-filled water bottle (from a lake, river,
     * cauldron, or waterlogged block) into Raw Water -- drinkable in a
     * pinch, but untreated: partial thirst restore and a real chance of
     * illness. Boiling it in a furnace (see WaterPurificationRecipe)
     * turns it into a proper Water Bottle. Returns false (and leaves the
     * item untouched) if it isn't actually a plain water potion -- a
     * real brewed potion sitting in the same hand never gets touched.
     */
    public boolean tagAsRawWater(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return false;
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;
        if (meta.getBasePotionType() != PotionType.WATER) return false;
        if (isWaterBottle(item) || isRawWater(item)) return false; // already tagged, nothing to do

        applyRawWaterLook(meta);
        item.setItemMeta(meta);
        return true;
    }

    private void applyWaterBottleLook(PotionMeta meta) {
        meta.setDisplayName("§bWater Bottle");
        meta.setLore(java.util.List.of("§7Right-click to drink and restore thirst."));
        meta.getPersistentDataContainer().set(waterBottleKey, PersistentDataType.BOOLEAN, true);
    }

    private void applyRawWaterLook(PotionMeta meta) {
        meta.setDisplayName("§7Raw Water");
        meta.setLore(java.util.List.of(
                "§7Untreated -- restores less thirst and may",
                "§7make you sick. Boil in a furnace to purify."
        ));
        meta.getPersistentDataContainer().set(rawWaterKey, PersistentDataType.BOOLEAN, true);
    }

    /**
     * A plain, unmodified water potion -- not our tagged Water Bottle or Raw
     * Water. Mainly relevant for a player (usually an admin) grabbing stock
     * water bottles straight from the creative inventory to hand out --
     * those still restore thirst at full strength, same as a proper Water
     * Bottle, so you're not forced to go through /nexussurvival give.
     */
    public boolean isPlainWaterPotion(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return false;
        if (isWaterBottle(item) || isRawWater(item)) return false; // already handled as one of ours
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;
        return meta.getBasePotionType() == PotionType.WATER;
    }

    public boolean isWaterBottle(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Boolean tag = item.getItemMeta().getPersistentDataContainer().get(waterBottleKey, PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(tag);
    }

    public boolean isRawWater(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Boolean tag = item.getItemMeta().getPersistentDataContainer().get(rawWaterKey, PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(tag);
    }
}
