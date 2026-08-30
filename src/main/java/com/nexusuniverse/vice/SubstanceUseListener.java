package com.nexusuniverse.vice;

import com.nexusuniverse.vice.substances.Substance;
import com.nexusuniverse.vice.substances.SubstanceItems;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Substances are now real items (sugar, snowballs, wheat, etc.), not
 * potions -- vanilla has no eating/drinking animation for those
 * materials, so PlayerItemConsumeEvent never fires for them. This
 * listens for a plain right-click instead, manually removes one from
 * the stack, and supplies its own use feedback (sound + particle +
 * message) to stand in for the animation that isn't available here.
 */
public class SubstanceUseListener implements Listener {

    private final SubstanceItems substanceItems;
    private final ViceDataManager viceData;
    private final ViceConfig config;

    public SubstanceUseListener(SubstanceItems substanceItems, ViceDataManager viceData, ViceConfig config) {
        this.substanceItems = substanceItems;
        this.viceData = viceData;
        this.config = config;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        Substance substance = substanceItems.readSubstance(inHand);
        if (substance == null) return;

        event.setCancelled(true);
        if (inHand.getAmount() > 1) {
            inHand.setAmount(inHand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        VicePlayerData data = viceData.get(player.getUniqueId());
        data.addSubstanceDose(substance, config.dosePerItem(substance));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.6f, 1.3f);
        player.getWorld().spawnParticle(Particle.DUST, player.getEyeLocation(), 6, 0.15, 0.15, 0.15,
                new Particle.DustOptions(Color.WHITE, 1.0f));
        player.sendMessage("§7You take " + config.displayName(substance) + ".");
    }
}
