package com.nexusuniverse.vice.guidebook;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class GuidebookListener implements Listener {

    private final GuidebookManager guidebookManager;
    private final GuidebookItem guidebookItem;

    public GuidebookListener(GuidebookManager guidebookManager, GuidebookItem guidebookItem) {
        this.guidebookManager = guidebookManager;
        this.guidebookItem = guidebookItem;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (guidebookManager.hasReceived(player.getUniqueId())) return;

        player.getInventory().addItem(guidebookItem.create());
        guidebookManager.markReceived(player.getUniqueId());
        player.sendMessage("§7You've been given a Vice Field Guide -- everything you need to know is in there.");
    }
}
