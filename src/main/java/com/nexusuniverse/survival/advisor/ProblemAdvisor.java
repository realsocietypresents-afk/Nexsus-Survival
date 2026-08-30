package com.nexusuniverse.survival.advisor;

import com.nexusuniverse.survival.config.NexusSurvivalConfig;
import com.nexusuniverse.survival.data.SurvivalPlayerData;
import org.bukkit.entity.Player;

/**
 * One central place for "you're taking damage -- here's what's going on, here's the fix"
 * messaging, used by every survival system that can actually hurt a player (dehydration,
 * radiation, critical disease, hypothermia, starvation, and the dirty-gear infection risk).
 * Always sends two lines back to back: a bold diagnosis line, then a plain solution line right
 * behind it -- exactly the "what's happening" + "what do I do about it" pairing that was asked
 * for, so it's never ambiguous why a player is hurting or what fixes it.
 *
 * Deliberately does NOT fire on every single damage tick -- that would flood chat, especially for
 * something like dehydration that reapplies every few seconds. Instead it fires once the moment a
 * problem starts hurting the player, then at most once every messages.reminder-interval-seconds
 * (config.yml, default 30s) for as long as the problem keeps going, tracked per player per
 * problem via a timestamp map on SurvivalPlayerData (see lastProblemMessageAt there). Call
 * clear() the instant a problem actually stops so its next onset reads as fresh -- an immediate
 * message again -- instead of silently waiting out whatever was left of the old timer.
 */
public class ProblemAdvisor {

    private final NexusSurvivalConfig config;

    public ProblemAdvisor(NexusSurvivalConfig config) {
        this.config = config;
    }

    /**
     * @param key       stable per-problem identifier ("dehydration", "radiation",
     *                  "disease-critical", "hypothermia", "starvation", "dirty-gear", ...) --
     *                  scoped per player via SurvivalPlayerData, so different players/problems
     *                  never share a throttle window.
     * @param diagnosis what's going on, sent first.
     * @param solution  the fix, sent immediately after as its own line.
     */
    public void notify(Player player, SurvivalPlayerData d, String key, String diagnosis, String solution) {
        if (!config.getBoolean("messages.enabled", true)) return;

        long now = System.currentTimeMillis();
        long intervalMs = 1000L * Math.max(5, config.getInt("messages.reminder-interval-seconds", 30));
        Long last = d.lastProblemMessageAt.get(key);
        if (last != null && now - last < intervalMs) return;

        d.lastProblemMessageAt.put(key, now);
        player.sendMessage(diagnosis);
        player.sendMessage(solution);
    }

    /**
     * Call the moment a problem actually stops (thirst restored, left the radiation zone, cured,
     * warmed back up, gear washed...) so if it starts again later it's treated as a brand-new
     * onset -- an immediate message -- rather than still being inside the previous reminder
     * window from before it was fixed.
     */
    public void clear(SurvivalPlayerData d, String key) {
        d.lastProblemMessageAt.remove(key);
    }
}
