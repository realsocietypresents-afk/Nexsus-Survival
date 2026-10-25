package com.nexusuniverse.survival.data;

import com.nexusuniverse.survival.config.NexusSurvivalConfig;
import com.nexusuniverse.survival.integration.PathGate;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the UUID -> SurvivalPlayerData map. All four managers (thirst,
 * radiation, hygiene, disease) read/write through this instead of keeping
 * their own maps, so join/quit cleanup happens in exactly one place.
 */
public class PlayerDataManager {

    private final NexusSurvivalConfig config;
    private final PathGate gate;
    private final Map<UUID, SurvivalPlayerData> data = new ConcurrentHashMap<>();

    public PlayerDataManager(NexusSurvivalConfig config, PathGate gate) {
        this.config = config;
        this.gate = gate;
    }

    /** True while this player has NexusSurvival switched off in NexusPath (see {@link PathGate}). */
    public boolean isSuspended(Player player) {
        return gate.isOff(player);
    }

    /** The player's state if it has been created already, otherwise null (never creates anything, never shows a bar). */
    public SurvivalPlayerData peek(Player player) {
        return data.get(player.getUniqueId());
    }

    /**
     * The player has switched NexusSurvival off: take the bars off their screen and clear what the
     * timers had built up (climate stages, radiation, faster thirst). Thirst itself is kept, so
     * switching it back on later carries on from where it was.
     */
    public void suspend(Player player) {
        SurvivalPlayerData d = peek(player);
        if (d == null) return;
        d.thirstBar.removePlayer(player);
        d.infectionBar.removePlayer(player);
        d.radiationBar.removePlayer(player);
        d.infectionBar.setVisible(false);
        d.radiationBar.setVisible(false);
        d.resetClimate();
        d.resetRadiation();
    }

    /** The player has switched NexusSurvival back on: show the bars again. */
    public void resume(Player player) {
        SurvivalPlayerData d = peek(player);
        if (d == null) return;
        d.thirstBar.addPlayer(player);
        d.infectionBar.addPlayer(player);
    }

    public SurvivalPlayerData get(Player player) {
        return data.computeIfAbsent(player.getUniqueId(), id -> {
            double maxThirst = config.thirstMax();
            BossBar thirstBar = Bukkit.createBossBar("§bThirst: " + (int) maxThirst + "/" + (int) maxThirst, BarColor.BLUE, BarStyle.SOLID);
            thirstBar.setProgress(1.0);
            // (a player who has NexusSurvival switched off in NexusPath never gets the bars; they
            // are added when they switch it back on)
            boolean showBars = !gate.isOff(player);
            if (showBars) thirstBar.addPlayer(player);

            BossBar radiationBar = Bukkit.createBossBar("§aRad-O2: 20/20", BarColor.GREEN, BarStyle.SOLID);
            radiationBar.setProgress(1.0);
            radiationBar.setVisible(false); // only shown while inside a radiation zone

            BossBar infectionBar = Bukkit.createBossBar("§cInfected", BarColor.PURPLE, BarStyle.SEGMENTED_6);
            infectionBar.setProgress(1.0);
            if (showBars) infectionBar.addPlayer(player);
            infectionBar.setVisible(false); // only shown while sick

            SurvivalPlayerData fresh = new SurvivalPlayerData(thirstBar, radiationBar, infectionBar);
            fresh.thirst = maxThirst; // start fully hydrated at whatever thirst.max is currently configured to, not a stale hardcoded 180
            return fresh;
        });
    }

    public void remove(Player player) {
        SurvivalPlayerData removed = data.remove(player.getUniqueId());
        if (removed != null) {
            removed.thirstBar.removeAll();
            removed.radiationBar.removeAll();
            removed.infectionBar.removeAll();
        }
    }

    public void clearAll() {
        for (SurvivalPlayerData d : data.values()) {
            d.thirstBar.removeAll();
            d.radiationBar.removeAll();
            d.infectionBar.removeAll();
        }
        data.clear();
    }
}
