package com.nexusuniverse.survival.advisor;

import com.nexusuniverse.survival.data.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Vanilla's own starvation damage (empty hunger bar dealing damage) doesn't go through any of
 * this plugin's tick loops -- the server deals it directly, on its own schedule. This just
 * listens for that one specific damage cause and routes it through the same ProblemAdvisor
 * diagnosis+solution messaging every other survival system uses, so "you're not eating" gets the
 * same clear in-game callout as dehydration, radiation, disease, and hypothermia -- instead of
 * being the one damage source in this whole plugin that stays silent about why it's happening.
 */
public class StarvationListener implements Listener {

    private final ProblemAdvisor advisor;
    private final PlayerDataManager playerData;

    public StarvationListener(ProblemAdvisor advisor, PlayerDataManager playerData) {
        this.advisor = advisor;
        this.playerData = playerData;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.STARVATION) return;
        if (!(event.getEntity() instanceof Player player)) return;

        advisor.notify(player, playerData.get(player), "starvation",
                "§4§lYou're starving! §cYour hunger bar is empty and it's dealing damage.",
                "§7Solution: eat some food to fill your hunger back up.");
    }
}
