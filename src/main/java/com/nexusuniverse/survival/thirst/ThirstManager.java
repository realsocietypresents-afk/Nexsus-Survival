package com.nexusuniverse.survival.thirst;

import com.nexusuniverse.survival.config.NexusSurvivalConfig;
import com.nexusuniverse.survival.data.PlayerDataManager;
import com.nexusuniverse.survival.data.SurvivalPlayerData;
import com.nexusuniverse.survival.integration.MoralityMessengerBridge;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ThirstManager {

    private final PlayerDataManager playerData;
    private final NexusSurvivalConfig config;
    private final MoralityMessengerBridge messenger;

    public ThirstManager(PlayerDataManager playerData, NexusSurvivalConfig config, MoralityMessengerBridge messenger) {
        this.playerData = playerData;
        this.config = config;
        this.messenger = messenger;
    }

    public void drink(Player player) {
        restore(player, config.thirstDrinkRestore());
        player.sendMessage("§bYou drink some water. Thirst restored.");
    }

    public void drinkRaw(Player player) {
        restore(player, config.thirstRawDrinkRestore());
        player.sendMessage("§7You drink some untreated water. Thirst partially restored.");
    }

    /** One sip from a Canteen -- weaker than a full Water Bottle, but doesn't use the item up. */
    public void drinkCanteenSip(Player player) {
        restore(player, config.thirstCanteenSipRestore());
        player.sendMessage("§bYou take a sip from your canteen. Thirst restored.");
    }

    private void restore(Player player, double amount) {
        SurvivalPlayerData d = playerData.get(player);
        d.thirst = Math.min(config.thirstMax(), d.thirst + amount);
    }

    /** Called every second from the central tick loop. */
    public void tick(Player player) {
        SurvivalPlayerData d = playerData.get(player);
        double maxThirst = config.thirstMax();

        // scaled by d.thirstDrainMultiplier (set by ClimateManager -- >1.0 during summer heat
        // exposure) rather than always incrementing by exactly 1, so "thirst drains faster in
        // summer" is a real rate change, not a separate second drain mechanism layered on top
        int decayIntervalSeconds = Math.max(1, config.thirstDecayIntervalSeconds());
        d.thirstTickCounter += d.thirstDrainMultiplier;
        if (d.thirstTickCounter >= decayIntervalSeconds) {
            d.thirstTickCounter -= decayIntervalSeconds; // subtract rather than reset, keeps fractional overshoot from a >1.0 multiplier accurate
            d.thirst = Math.max(0, d.thirst - 1);
        }

        if (d.thirst <= 0) {
            int dehydrationDamageIntervalSeconds = Math.max(1, config.thirstDehydrationDamageIntervalSeconds());
            d.dehydrationDamageTickCounter++;
            if (d.dehydrationDamageTickCounter >= dehydrationDamageIntervalSeconds) {
                d.dehydrationDamageTickCounter = 0;
                player.damage(1.0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0, true, false));
                messenger.announce(player, "survival.dehydration",
                        "You're dehydrated and taking damage.",
                        "Drink a Water Bottle, take a sip from your Canteen, or purify some Raw Water in a furnace.");
            }
        } else {
            d.dehydrationDamageTickCounter = 0;
        }

        checkThirstAlert(player, d);
        updateBar(player, d, maxThirst);
    }

    /** Fires ONE highlighted chat message the moment thirst crosses down to config.thirst.alert.threshold, resets once thirst climbs back above it so it can fire again next time. */
    private void checkThirstAlert(Player player, SurvivalPlayerData d) {
        if (!config.thirstAlertEnabled()) return;
        int threshold = config.thirstAlertThreshold();

        if (d.thirst <= threshold) {
            if (!d.thirstAlertFired) {
                d.thirstAlertFired = true;
                player.sendMessage(ChatColor.GRAY + "Your thirst level is " + ChatColor.GOLD + "" + ChatColor.BOLD
                        + threshold + ChatColor.RESET + ChatColor.GRAY + ".");
            }
        } else {
            d.thirstAlertFired = false;
        }
    }

    private void updateBar(Player player, SurvivalPlayerData d, double maxThirst) {
        double progress = d.thirst / maxThirst;
        d.thirstBar.setProgress(Math.max(0, Math.min(1, progress)));
        d.thirstBar.setTitle("§bThirst: " + (int) d.thirst + "/" + (int) maxThirst);
        d.thirstBar.setColor(d.thirst <= maxThirst * 0.2 ? BarColor.RED : BarColor.BLUE);
    }
}
