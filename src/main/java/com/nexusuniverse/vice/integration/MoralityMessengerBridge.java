package com.nexusuniverse.vice.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Soft-dependency bridge to NexusMorality's Problem+Solution chat messenger
 * (NexusMoralityMessengerApi), the same reflection + Bukkit ServicesManager shape every other
 * Nexus plugin's own integration/MoralityMessengerBridge.java uses (NexusVitals, NexusCombat,
 * NexusSeasons, NexusSurvival). NexusVice never gets NexusMorality's classes on its own compile
 * classpath - it only knows the fully-qualified class name and the one method signature it
 * needs, so this keeps working (announce() just becomes a no-op) whether or not NexusMorality
 * is installed at all.
 *
 * Only ONE place in NexusVice actually deals direct, scripted damage to a player:
 * ViceEffectManager#tickBlackoutDamage, the recurring pulse that hurts anyone currently in an
 * active overdose/alcohol-poisoning blackout. Everything else in this plugin -- the tiered
 * intoxication effects (Slowness/Nausea/Blindness/etc.), vomiting, stumbling, food-level drain,
 * comedowns -- is a debuff, not damage, so it's deliberately left alone.
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
     * Looks up NexusMorality's messenger service. Call once from onEnable - after registering
     * NexusMorality as a softdepend in plugin.yml so, if it's installed, it's already loaded and
     * has already registered its service by the time this runs.
     */
    public void resolve() {
        try {
            Class<?> apiClass = Class.forName(API_CLASS_NAME);
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) {
                plugin.getLogger().info("NexusVice: NexusMorality's messenger service isn't registered "
                        + "(plugin not installed, or not enabled yet) - overdose blackout damage messages "
                        + "stay standalone this session.");
                return;
            }
            this.service = registration.getProvider();
            this.announceMethod = apiClass.getMethod("announce", Player.class, String.class, String.class, String.class);
            plugin.getLogger().info("NexusVice: found NexusMorality's Problem+Solution messenger - "
                    + "overdose blackout damage will announce there too.");
        } catch (ClassNotFoundException e) {
            // NexusMorality isn't installed at all - expected and fine, NexusVice works standalone.
        } catch (ReflectiveOperationException | RuntimeException e) {
            plugin.getLogger().warning("NexusVice: found NexusMorality but couldn't bind to its messenger "
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
                plugin.getLogger().warning("NexusVice: a call into NexusMorality's messenger failed ("
                        + e + ") - won't repeat this warning again this session.");
            }
        }
    }
}
