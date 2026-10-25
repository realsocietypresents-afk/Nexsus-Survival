package com.nexusuniverse.survival.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Soft-dependency bridge to NexusMorality's Problem+Solution chat messenger
 * (NexusMoralityMessengerApi), the same reflection + Bukkit ServicesManager
 * shape every other Nexus plugin's own integration/MoralityMessengerBridge.java
 * uses (NexusVitals, NexusCombat, NexusSeasons). NexusSurvival never gets
 * NexusMorality's classes on its own compile classpath - it only knows the
 * fully-qualified class name and the one method signature it needs, so this
 * keeps working (announce() just becomes a no-op) whether or not NexusMorality
 * is installed at all.
 *
 * Only the five places in NexusSurvival that actually deal direct, scripted
 * damage to a player call through here: ThirstManager's dehydration tick,
 * ClimateManager's hypothermia and heat-stroke max-stage ticks, RadiationManager's
 * suffocation tick, and DiseaseManager's critical-severity symptom tick. Everything
 * else in this plugin (debuffs, hygiene, the mob-dismemberment/crawler/bleed system
 * -- which only ever damages MOBS the player shoots, never the player themselves --
 * feral/TNT zombies and their vanilla attack/explosion damage) is either not real
 * damage, damage the player deals outward, or damage already covered generically by
 * vanilla's own EntityDamageEvent cause (which NexusMorality's own listener already
 * handles), so it's deliberately left alone.
 */
public final class MoralityMessengerBridge {

    private static final String API_CLASS_NAME = "com.nexusuniverse.morality.messages.NexusMoralityMessengerApi";

    private final JavaPlugin plugin;
    private Object service;
    private Method announceMethod;
    private boolean warnedAboutInvokeFailure;

    public MoralityMessengerBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Looks up NexusMorality's messenger service. Call once from onEnable -
     * after registering NexusMorality as a softdepend in plugin.yml so, if
     * it's installed, it's already loaded and has already registered its
     * service by the time this runs.
     */
    public void resolve() {
        try {
            Class<?> apiClass = Class.forName(API_CLASS_NAME);
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) {
                plugin.getLogger().info("NexusSurvival: NexusMorality's messenger service isn't registered "
                        + "(plugin not installed, or not enabled yet) - dehydration/climate/radiation/disease "
                        + "damage messages stay standalone this session.");
                return;
            }
            this.service = registration.getProvider();
            this.announceMethod = apiClass.getMethod("announce", Player.class, String.class, String.class, String.class);
            plugin.getLogger().info("NexusSurvival: found NexusMorality's Problem+Solution messenger - "
                    + "dehydration, hypothermia, heat stroke, radiation, and critical disease damage will "
                    + "announce there too.");
        } catch (ClassNotFoundException e) {
            // NexusMorality isn't installed at all - expected and fine, NexusSurvival works standalone.
        } catch (ReflectiveOperationException | RuntimeException e) {
            plugin.getLogger().warning("NexusSurvival: found NexusMorality but couldn't bind to its messenger "
                    + "API (" + e + ") - staying standalone this session.");
        }
    }

    /** Safe to call unconditionally - a no-op whenever resolve() didn't find a working service. */
    public void announce(Player player, String causeKey, String problem, String solution) {
        if (service == null || announceMethod == null) return;
        try {
            announceMethod.invoke(service, player, causeKey, problem, solution);
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warnedAboutInvokeFailure) {
                warnedAboutInvokeFailure = true;
                plugin.getLogger().warning("NexusSurvival: a call into NexusMorality's messenger failed ("
                        + e + ") - won't repeat this warning again this session.");
            }
        }
    }
}
