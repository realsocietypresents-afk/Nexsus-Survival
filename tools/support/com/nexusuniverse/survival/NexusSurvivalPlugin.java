package com.nexusuniverse.survival;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Test-only stand-in for the real plugin class (which pulls in every mob and listener class and
 * so cannot be compiled against the small hand-written API stubs). SunCapItem and RadiationItems
 * only need a plugin to make their item keys.
 */
public class NexusSurvivalPlugin extends JavaPlugin {
    /** What plugin.yml says; the PathGate looks for this name (lower case) in what NexusPath publishes. */
    @Override
    public String getName() {
        return "NexusSurvival";
    }
}
