package com.nexusuniverse.vice;

import com.nexusuniverse.vice.alcohol.AlcoholBrand;
import com.nexusuniverse.vice.alcohol.AlcoholItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/** Alcohol only -- it's still a real drinkable potion bottle, so the vanilla drink animation/event works fine for it. Substances moved to SubstanceUseListener. */
public class ConsumeListener implements Listener {

    private final AlcoholItems alcoholItems;
    private final ViceDataManager viceData;
    private final ViceConfig config;

    public ConsumeListener(AlcoholItems alcoholItems, ViceDataManager viceData, ViceConfig config) {
        this.alcoholItems = alcoholItems;
        this.viceData = viceData;
        this.config = config;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();

        AlcoholBrand brand = alcoholItems.readBrand(event.getItem());
        if (brand == null) return;

        VicePlayerData data = viceData.get(player.getUniqueId());
        data.addAlcoholDose(config.dosePerItem(brand));
        player.sendMessage("§7You drink " + config.displayName(brand) + ".");
    }
}
