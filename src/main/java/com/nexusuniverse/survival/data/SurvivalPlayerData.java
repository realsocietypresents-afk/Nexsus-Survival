package com.nexusuniverse.survival.data;

import com.nexusuniverse.survival.disease.Disease;
import org.bukkit.boss.BossBar;

/**
 * All per-player state for the four survival systems, kept in one place
 * so managers don't each need their own map + join/quit bookkeeping.
 */
public class SurvivalPlayerData {

    // thirst: 0-thirst.max (config-driven, default 267 -- see NexusSurvivalConfig#thirstMax).
    // 180.0 here is just a harmless placeholder; PlayerDataManager overwrites it with the real
    // configured max the moment a fresh SurvivalPlayerData is actually created for a player.
    public double thirst = 180.0;
    public double thirstTickCounter = 0;
    public int dehydrationDamageTickCounter = 0;
    // >1.0 while a hot/exposed summer condition is active -- ThirstManager scales its decay by this
    public double thirstDrainMultiplier = 1.0;
    // one-shot guard for the thirst.alert.threshold chat message -- set the moment thirst crosses
    // down to/through the threshold, cleared once thirst rises back above it, so it fires again
    // next time rather than repeating every tick while sitting at/below the threshold
    public boolean thirstAlertFired = false;

    // climate: winter exposure -> hypothermia, summer exposure -> heat exhaustion. Both accumulate
    // while the triggering condition holds and recede while it doesn't, in discrete stages rather
    // than a single on/off state, so brief exposure (running between buildings) doesn't hit a
    // player as hard as staying out in it.
    public int hypothermiaStage = 0;
    public int hypothermiaTickCounter = 0;
    public int heatStage = 0;
    // real-seconds countdown used only for RECOVERY once heat exposure stops (see
    // ClimateManager#decayHeat) -- building UP a stage is now paced by actual in-game time
    // instead, tracked by the two fields below.
    public int heatTickCounter = 0;
    // accumulated IN-GAME ticks (World#getFullTime(), the real day/night clock NexusSeasons
    // drives) of continuous unshaded, uncapped summer exposure -- paused (not reset) the moment
    // that stops being true, so a brief step into shade doesn't erase a near-complete stage.
    public long heatExposureGameTicks = 0;
    // last-seen World#getFullTime() for this player, so ClimateManager can compute how many
    // in-game ticks actually elapsed since the last check rather than assuming a fixed rate.
    // -1 means "not sampled yet" (fresh join) -- the first tick after that credits zero elapsed
    // time rather than a bogus jump from 0.
    public long lastSeenWorldFullTime = -1;
    // spring/fall's periodic "pleasant weather" buff roll -- counts up to the configured
    // check-interval, independent of the winter/summer counters above
    public int mildWeatherTickCounter = 0;

    // radiation "rad-oxygen": 0-20, drains in radiation zones, regens outside
    public double radOxygen = 20.0;
    public int radTickCounter = 0;

    // hygiene: 0 (clean) - 100 (filthy)
    public double dirtiness = 0.0;
    public int hygieneTickCounter = 0;

    // disease: null if healthy
    public Disease infection = null;
    public int symptomTickCounter = 0;
    public int contagionTickCounter = 0;
    public int biomeExposureTickCounter = 0;

    // how long an infection has gone uncured, in escalation tiers: 0=Mild, 1=Moderate, 2=Severe, 3=Critical
    public int infectionSeverity = 0;
    public int severityTickCounter = 0;

    // set on cure(), checked on infect() -- immune to catching anything new until this tick passes
    public long immuneUntilTick = 0;

    // UI
    public final BossBar thirstBar;
    public final BossBar radiationBar;
    public final BossBar infectionBar;

    public SurvivalPlayerData(BossBar thirstBar, BossBar radiationBar, BossBar infectionBar) {
        this.thirstBar = thirstBar;
        this.radiationBar = radiationBar;
        this.infectionBar = infectionBar;
    }
}
