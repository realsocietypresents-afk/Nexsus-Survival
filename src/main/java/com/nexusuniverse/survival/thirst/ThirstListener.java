package com.nexusuniverse.survival.thirst;

import com.nexusuniverse.survival.NexusSurvivalPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class ThirstListener implements Listener {

    private final NexusSurvivalPlugin plugin;
    private final Random random = new Random();

    public ThirstListener(NexusSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (plugin.getThirstItems().isWaterBottle(item)) {
            plugin.getThirstManager().drink(player);
            return;
        }

        if (plugin.getThirstItems().isPlainWaterPotion(item)) {
            plugin.getThirstManager().drink(player);
            return;
        }

        if (plugin.getThirstItems().isRawWater(item)) {
            plugin.getThirstManager().drinkRaw(player);
            double chance = plugin.getNexusSurvivalConfig().getDouble("disease.raw-water-contamination-chance", 0.15)
                    * plugin.getNexusSurvivalConfig().diseaseChanceMultiplier();
            if (random.nextDouble() < chance) {
                plugin.getDiseaseManager().infectRandom(player, "the untreated water you drank");
            }
        }
    }

    /**
     * Catches the moment a player fills a glass bottle from a real water
     * source (lake, river, cauldron, waterlogged block) and upgrades the
     * result into Raw Water -- untreated, drinkable in a pinch, but not
     * as good as a properly boiled Water Bottle. Checked one tick later:
     * the actual glass-bottle-to-potion swap happens as part of
     * vanilla's own handling of this same right-click, not something we
     * get to control the timing of, so we let that finish first and then
     * look at what ended up in the player's hand.
     */
    @EventHandler
    public void onBottleFill(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null || !isWaterSource(clicked)) return;

        ItemStack itemInHand = event.getItem();
        if (itemInHand == null || itemInHand.getType() != Material.GLASS_BOTTLE) return;

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        if (hand == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack current = hand == EquipmentSlot.OFF_HAND
                    ? player.getInventory().getItemInOffHand()
                    : player.getInventory().getItemInMainHand();
            plugin.getThirstItems().tagAsRawWater(current);
        });
    }

    private boolean isWaterSource(Block block) {
        if (block.getType() == Material.WATER || block.getType() == Material.WATER_CAULDRON) return true;
        return block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
    }
}
