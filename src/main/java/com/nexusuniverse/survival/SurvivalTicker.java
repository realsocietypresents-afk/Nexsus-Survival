package com.nexusuniverse.survival;

import com.nexusuniverse.survival.data.PlayerDataManager;
import com.nexusuniverse.survival.disease.DiseaseManager;
import com.nexusuniverse.survival.integration.PathGate;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The once-a-second pass over every online player (thirst, radiation, hygiene, disease, climate).
 *
 * A player who has Thirst, Disease & Climate switched off in NexusPath (/path) is skipped
 * entirely. The moment they switch it off, whatever it had done to them is cleared (any illness,
 * the cold and heat stages, the bars); the moment they switch it back on, the bars come back.
 */
final class SurvivalTicker {

    private final PathGate gate;
    private final DiseaseManager diseases;
    private final PlayerDataManager playerData;
    private final Logger log;

    SurvivalTicker(PathGate gate, DiseaseManager diseases, PlayerDataManager playerData, Logger log) {
        this.gate = gate;
        this.diseases = diseases;
        this.playerData = playerData;
        this.log = log;
    }

    /** Runs every step, in order, for every player this plugin applies to. One failing step is logged and does not stop the rest. */
    void tickPlayers(Iterable<? extends Player> online, Map<String, Consumer<Player>> steps) {
        for (Player player : online) {
            if (!appliesTo(player)) continue;
            for (Map.Entry<String, Consumer<Player>> step : steps.entrySet()) {
                try {
                    step.getValue().accept(player);
                } catch (Throwable t) {
                    log.log(Level.WARNING, "NexusSurvival: error ticking '" + step.getKey() + "' -- this pass skipped it, will retry next tick.", t);
                }
            }
        }
    }

    /** False while the player has it switched off. Also does the clean-up / restore on the second it changes. */
    boolean appliesTo(Player player) {
        try {
            switch (gate.update(player)) {
                case JUST_OFF -> {
                    diseases.dropInfection(player);
                    playerData.suspend(player);
                    return false;
                }
                case OFF -> {
                    return false;
                }
                case JUST_ON -> {
                    playerData.resume(player);
                    return true;
                }
                default -> {
                    return true;
                }
            }
        } catch (RuntimeException e) {
            log.log(Level.WARNING, "NexusSurvival: could not read " + player.getName() + "'s NexusPath choice -- treating survival as on for them.", e);
            return true;
        }
    }
}
