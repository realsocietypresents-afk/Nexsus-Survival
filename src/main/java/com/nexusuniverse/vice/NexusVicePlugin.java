package com.nexusuniverse.vice;

import com.nexusuniverse.vice.alcohol.AlcoholBrand;
import com.nexusuniverse.vice.alcohol.AlcoholItems;
import com.nexusuniverse.vice.alcohol.AlcoholType;
import com.nexusuniverse.vice.effects.ViceEffectManager;
import com.nexusuniverse.vice.integration.MoralityMessengerBridge;
import com.nexusuniverse.vice.guidebook.GuidebookItem;
import com.nexusuniverse.vice.guidebook.GuidebookListener;
import com.nexusuniverse.vice.guidebook.GuidebookManager;
import com.nexusuniverse.vice.substances.Substance;
import com.nexusuniverse.vice.substances.SubstanceItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class NexusVicePlugin extends JavaPlugin implements NexusViceAPI {

    private ViceConfig config;
    private ViceDataManager viceData;
    private ViceEffectManager effectManager;
    private MoralityMessengerBridge moralityMessenger;
    private SubstanceItems substanceItems;
    private AlcoholItems alcoholItems;

    @Override
    public void onEnable() {
        this.config = new ViceConfig(this);
        this.viceData = new ViceDataManager(this);
        this.moralityMessenger = new MoralityMessengerBridge(this);
        this.moralityMessenger.resolve();
        this.effectManager = new ViceEffectManager(config, viceData, moralityMessenger);

        getServer().getServicesManager().register(NexusViceAPI.class, this, this, ServicePriority.Normal);

        this.substanceItems = new SubstanceItems(this, config);
        this.alcoholItems = new AlcoholItems(this, config);
        ViceRecipes.registerAll(this, substanceItems, alcoholItems);

        getServer().getPluginManager().registerEvents(new ConsumeListener(alcoholItems, viceData, config), this);
        getServer().getPluginManager().registerEvents(new SubstanceUseListener(substanceItems, viceData, config), this);

        GuidebookItem guidebookItem = new GuidebookItem();
        GuidebookManager guidebookManager = new GuidebookManager(this);
        getServer().getPluginManager().registerEvents(new GuidebookListener(guidebookManager, guidebookItem), this);

        getCommand("vice").setExecutor(new ViceCommand(config, viceData, substanceItems, alcoholItems, guidebookItem, guidebookManager));

        // main effect tick: decay, tiered effects, vomit rolls, crash detection, combos
        long effectIntervalTicks = 20L * config.tickIntervalSeconds();
        getServer().getScheduler().runTaskTimer(this, () -> {
            safeTick("effects", () -> {
                effectManager.advanceClock();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    effectManager.tickEffects(player);
                }
            });
        }, effectIntervalTicks, effectIntervalTicks);

        // blackout damage: its own interval, only touches players currently overdosing
        long blackoutIntervalTicks = 20L * config.blackoutPulseIntervalSeconds();
        getServer().getScheduler().runTaskTimer(this, () -> {
            safeTick("blackout-damage", () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    effectManager.tickBlackoutDamage(player);
                }
            });
        }, blackoutIntervalTicks, blackoutIntervalTicks);

        getLogger().info("NexusVice enabled -- " + Substance.values().length + " substances, "
                + AlcoholBrand.values().length + " alcohol brands across " + AlcoholType.values().length
                + " base types, all invented, none of them real.");
    }

    /** Isolates each scheduled pass -- a failure here is logged, not allowed to abort other systems sharing the scheduler. */
    private void safeTick(String subsystem, Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "NexusVice: error ticking '" + subsystem + "' -- this pass skipped it, will retry next tick.", t);
        }
    }

    @Override
    public void onDisable() {
        // active dose/blackout state is intentionally NOT persisted -- it's meant to be
        // transient, not something worth surviving a restart. Overdose counts (a lifetime
        // achievement stat, used by NexusLegends' Fame system) ARE persisted, but that's
        // handled incrementally by ViceDataManager as it happens, not here.
    }

    @Override
    public int getOverdoseCount(UUID playerId) {
        return viceData.getOverdoseCount(playerId);
    }

    // --- NexusViceAPI: shop catalog surface ---

    @Override
    public List<String> allSubstanceIds() {
        List<String> ids = new ArrayList<>();
        for (Substance substance : Substance.values()) ids.add(substance.configKey());
        return ids;
    }

    @Override
    public String substanceDisplayName(String id) {
        Substance substance = substanceById(id);
        return substance == null ? null : config.displayName(substance);
    }

    @Override
    public String substanceCategoryName(String id) {
        Substance substance = substanceById(id);
        return substance == null ? null : substance.category().name();
    }

    @Override
    public boolean substanceHasOverdoseRisk(String id) {
        Substance substance = substanceById(id);
        return substance != null && config.hasOverdoseRisk(substance);
    }

    @Override
    public double substanceOverdoseThreshold(String id) {
        Substance substance = substanceById(id);
        return substance == null ? 0 : config.overdoseThreshold(substance);
    }

    @Override
    public ItemStack createSubstanceItem(String id) {
        Substance substance = substanceById(id);
        return substance == null ? null : substanceItems.create(substance);
    }

    @Override
    public List<String> allAlcoholBrandIds() {
        List<String> ids = new ArrayList<>();
        for (AlcoholBrand brand : AlcoholBrand.values()) ids.add(brand.configKey());
        return ids;
    }

    @Override
    public String alcoholDisplayName(String id) {
        AlcoholBrand brand = brandById(id);
        return brand == null ? null : config.displayName(brand);
    }

    @Override
    public String alcoholTypeName(String id) {
        AlcoholBrand brand = brandById(id);
        return brand == null ? null : brand.type().name();
    }

    @Override
    public String alcoholQualityName(String id) {
        AlcoholBrand brand = brandById(id);
        return brand == null ? null : brand.quality().name();
    }

    @Override
    public ItemStack createAlcoholItem(String id) {
        AlcoholBrand brand = brandById(id);
        return brand == null ? null : alcoholItems.create(brand);
    }

    private Substance substanceById(String id) {
        if (id == null) return null;
        for (Substance substance : Substance.values()) {
            if (substance.configKey().equalsIgnoreCase(id)) return substance;
        }
        return null;
    }

    private AlcoholBrand brandById(String id) {
        if (id == null) return null;
        for (AlcoholBrand brand : AlcoholBrand.values()) {
            if (brand.configKey().equalsIgnoreCase(id)) return brand;
        }
        return null;
    }

    public ViceDataManager getViceData() {
        return viceData;
    }

    public ViceConfig getViceConfig() {
        return config;
    }
}
