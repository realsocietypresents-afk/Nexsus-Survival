package com.nexusuniverse.survival.integration;

import com.nexusuniverse.survival.config.NexusSurvivalConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets a player switch NexusSurvival off for themselves through NexusPath ({@code /path}).
 *
 * NexusPath hides a system by blocking that plugin's commands and event handlers, but it cannot
 * stop a plugin's own repeating timer, and nearly everything here (thirst draining, the thirst
 * bar, climate stages, disease symptoms and spread, radiation) runs from one. So NexusPath
 * publishes what each player has hidden, and this class is how NexusSurvival reads it: the
 * player's persistent data key {@code nexuspath:hidden} holds a comma separated list of lower
 * case plugin names while anything is hidden for them.
 *
 * Only the persistent data is read, never the {@code nexuspath.hidden.*} permission: a server
 * that gives staff a wildcard permission would otherwise look as if every staff member had
 * everything hidden.
 *
 * With {@code path.respect-nexuspath: false} in config.yml the whole check is off and everyone
 * plays with NexusSurvival on, whatever they chose in /path.
 */
public final class PathGate {

    /** What {@link #update} found for one player compared with the last time it looked. */
    public enum State {
        /** NexusSurvival applies to the player (as it did last time). */
        ON,
        /** The player has it switched off (as they did last time). */
        OFF,
        /** The player has just switched it off: clean up what it was doing to them. */
        JUST_OFF,
        /** The player has just switched it back on. */
        JUST_ON
    }

    private static final NamespacedKey HIDDEN = NamespacedKey.fromString("nexuspath:hidden");

    private final NexusSurvivalConfig config;
    private final String token;
    private final Set<UUID> off = ConcurrentHashMap.newKeySet();

    public PathGate(Plugin plugin, NexusSurvivalConfig config) {
        this.config = config;
        this.token = plugin.getName().toLowerCase(Locale.ROOT);
    }

    /** True while this player has NexusSurvival switched off in NexusPath. Reads the current value every call. */
    public boolean isOff(Player player) {
        if (player == null || HIDDEN == null || !config.getBoolean("path.respect-nexuspath", true)) {
            return false;
        }
        String hidden = player.getPersistentDataContainer().get(HIDDEN, PersistentDataType.STRING);
        if (hidden == null || hidden.isEmpty()) {
            return false;
        }
        for (String part : hidden.split(",")) {
            if (token.equals(part.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /** Looks at the player now and says whether that differs from the last time. Call once a second per player. */
    public State update(Player player) {
        boolean now = isOff(player);
        boolean before = off.contains(player.getUniqueId());
        if (now == before) {
            return now ? State.OFF : State.ON;
        }
        if (now) {
            off.add(player.getUniqueId());
            return State.JUST_OFF;
        }
        off.remove(player.getUniqueId());
        return State.JUST_ON;
    }

    /** Forgets a player who left. */
    public void forget(Player player) {
        off.remove(player.getUniqueId());
    }
}
