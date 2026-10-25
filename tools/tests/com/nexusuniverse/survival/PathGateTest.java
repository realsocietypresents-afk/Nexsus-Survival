package com.nexusuniverse.survival;

import com.nexusuniverse.survival.climate.ClimateManager;
import com.nexusuniverse.survival.climate.SunCapItem;
import com.nexusuniverse.survival.config.NexusSurvivalConfig;
import com.nexusuniverse.survival.data.PlayerDataManager;
import com.nexusuniverse.survival.data.SurvivalPlayerData;
import com.nexusuniverse.survival.disease.Disease;
import com.nexusuniverse.survival.disease.DiseaseManager;
import com.nexusuniverse.survival.hygiene.HygieneManager;
import com.nexusuniverse.survival.integration.MoralityMessengerBridge;
import com.nexusuniverse.survival.integration.PathGate;
import com.nexusuniverse.survival.radiation.RadiationItems;
import com.nexusuniverse.survival.radiation.RadiationManager;
import com.nexusuniverse.survival.seasons.SeasonBridge;
import com.nexusuniverse.survival.thirst.ThirstManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * A player who switches Thirst, Disease & Climate off in NexusPath really has it off: the once-a-second
 * pass skips them, nothing infects them, no bar is put on their screen, and what they already had is
 * cleared the moment they switch it off. Plain-main checks against the hand-written stubs.
 */
public class PathGateTest {

    static int checks;
    static int failures;

    static void expect(boolean ok, String what) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + what);
        }
    }

    static final org.bukkit.World WORLD = new org.bukkit.World("world");
    static final NamespacedKey HIDDEN = NamespacedKey.fromString("nexuspath:hidden");

    static final class Who extends Player {
        final String name;
        final List<String> messages = new ArrayList<>();
        boolean brokenData;

        Who(String name) {
            this.name = name;
        }

        @Override public String getName() { return name; }
        @Override public void sendMessage(String message) { messages.add(message); }
        @Override public boolean isOnline() { return true; }

        @Override
        public org.bukkit.persistence.PersistentDataContainer getPersistentDataContainer() {
            if (brokenData) {
                throw new IllegalStateException("broken");
            }
            return super.getPersistentDataContainer();
        }

        /** What NexusPath publishes: a comma separated list of hidden plugins (null clears it). */
        void hide(String list) {
            if (list == null) {
                getPersistentDataContainer().remove(HIDDEN);
            } else {
                getPersistentDataContainer().set(HIDDEN, PersistentDataType.STRING, list);
            }
        }
    }

    static final class Config extends NexusSurvivalConfig {
        boolean respect = true;
        boolean clearOnDeath = true;
        double chanceMultiplier = 0.35;

        Config(org.bukkit.plugin.java.JavaPlugin plugin) {
            super(plugin);
        }

        @Override
        public boolean getBoolean(String path, boolean fallback) {
            if (path.equals("disease.clear-on-death")) {
                return clearOnDeath;
            }
            return path.equals("path.respect-nexuspath") ? respect : super.getBoolean(path, fallback);
        }

        @Override
        public double getDouble(String path, double fallback) {
            return path.equals("disease.chance-multiplier") ? chanceMultiplier : super.getDouble(path, fallback);
        }
    }

    static final class Rig {
        final NexusSurvivalPlugin plugin = new NexusSurvivalPlugin();
        final Config config = new Config(plugin);
        final PathGate gate = new PathGate(plugin, config);
        final PlayerDataManager data = new PlayerDataManager(config, gate);
        final MoralityMessengerBridge messenger = new MoralityMessengerBridge(plugin);
        final DiseaseManager diseases = new DiseaseManager(data, config, new SeasonBridge(), messenger, gate);
        final ThirstManager thirst = new ThirstManager(data, config, messenger);
        final RadiationManager radiation = new RadiationManager(data, new RadiationItems(plugin), messenger);
        final HygieneManager hygiene = new HygieneManager(data, diseases, config);
        final ClimateManager climate = new ClimateManager(data, config, new SeasonBridge(), new SunCapItem(plugin), messenger);
        final List<String> logged = new ArrayList<>();
        final Logger log = new Logger("test", null) {
            @Override
            public void log(java.util.logging.Level level, String msg, Throwable thrown) {
                logged.add(msg);
            }
        };
        final SurvivalTicker ticker = new SurvivalTicker(gate, diseases, data, log);
        final Map<String, Consumer<Player>> steps = new LinkedHashMap<>();

        Rig() {
            steps.put("thirst", thirst::tick);
            steps.put("radiation", radiation::tick);
            steps.put("hygiene", hygiene::tick);
            steps.put("disease", diseases::tick);
            steps.put("climate", climate::tick);
        }

        Who join(String name) {
            Who p = new Who(name);
            p.stubSetLocation(new org.bukkit.Location(WORLD, 0, 64, 0));
            Bukkit.PLAYERS.put(p.getUniqueId(), p);
            Bukkit.ENTITIES.put(p.getUniqueId(), p);
            return p;
        }

        /** One second of the plugin's own timer (the per-player part, then the server-wide disease part). */
        void second() {
            ticker.tickPlayers(Bukkit.getOnlinePlayers(), steps);
            diseases.tickGlobal(Bukkit.getOnlinePlayers());
        }

        void seconds(int n) {
            for (int i = 0; i < n; i++) {
                second();
            }
        }
    }

    public static void main(String[] args) {
        reading();
        bars();
        infections();
        theSecond();
        switching();
        broken();
        dying();
        System.out.println(checks + " checks, " + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }

    static void clearServer() {
        Bukkit.PLAYERS.clear();
        Bukkit.ENTITIES.clear();
    }

    // ------------------------------------------------------------------ reading the choice

    static void reading() {
        clearServer();
        Rig r = new Rig();
        Who p = r.join("Ann");
        expect(!r.gate.isOff(p), "a player NexusPath has published nothing for has it on (the stub's hasPermission says yes to everything: permissions are not read)");
        p.hide("nexussurvival");
        expect(r.gate.isOff(p), "listed: off");
        p.hide("nexusfarming,nexussurvival,nexusgrave");
        expect(r.gate.isOff(p), "in the middle of a list: off");
        p.hide("NexusSurvival");
        expect(r.gate.isOff(p), "upper case is the same plugin");
        p.hide(" nexusgrave , nexussurvival ");
        expect(r.gate.isOff(p), "spaces around names do not matter");
        p.hide("nexussurvivalx,xnexussurvival,nexussurviva");
        expect(!r.gate.isOff(p), "other plugins with a similar name are not it");
        p.hide("nexusfarming");
        expect(!r.gate.isOff(p), "something else hidden leaves it on");
        p.hide("");
        expect(!r.gate.isOff(p), "an empty list leaves it on");
        p.hide(null);
        expect(!r.gate.isOff(p), "nothing published leaves it on");
        expect(!r.gate.isOff(null), "no player is not off");

        p.hide("nexussurvival");
        r.config.respect = false;
        expect(!r.gate.isOff(p), "with path.respect-nexuspath false the choice is ignored");
        r.config.respect = true;
        expect(r.gate.isOff(p), "and back");

        // the states a once-a-second pass sees
        p.hide(null);
        expect(r.gate.update(p) == PathGate.State.ON, "first look, on");
        expect(r.gate.update(p) == PathGate.State.ON, "still on");
        p.hide("nexussurvival");
        expect(r.gate.update(p) == PathGate.State.JUST_OFF, "switched off");
        expect(r.gate.update(p) == PathGate.State.OFF, "still off");
        p.hide(null);
        expect(r.gate.update(p) == PathGate.State.JUST_ON, "switched back on");
        expect(r.gate.update(p) == PathGate.State.ON, "and on");
        p.hide("nexussurvival");
        r.gate.update(p);
        r.gate.forget(p);
        expect(r.gate.update(p) == PathGate.State.JUST_OFF, "a player who left and came back off is found off again");
        Who fresh = r.join("Bob");
        fresh.hide("nexussurvival");
        expect(r.gate.update(fresh) == PathGate.State.JUST_OFF, "a player who joins with it off is cleaned up once");
    }

    // ------------------------------------------------------------------ the bars

    static void bars() {
        clearServer();
        Rig r = new Rig();
        Who on = r.join("On");
        Who off = r.join("Off");
        off.hide("nexussurvival");
        expect(r.data.peek(on) == null, "nothing exists for a player before it is needed");
        SurvivalPlayerData a = r.data.get(on);
        SurvivalPlayerData b = r.data.get(off);
        expect(a.thirstBar.getPlayers().contains(on) && a.infectionBar.getPlayers().contains(on), "a player with it on gets the thirst and illness bars");
        expect(b.thirstBar.getPlayers().isEmpty() && b.infectionBar.getPlayers().isEmpty(), "a player with it off does not, however their state came to be made");
        expect(r.data.isSuspended(off) && !r.data.isSuspended(on), "isSuspended follows the choice");

        // switching off removes what was there; thirst itself is kept
        a.thirst = 100;
        a.hypothermiaStage = 3;
        a.heatStage = 2;
        a.heatExposureGameTicks = 500;
        a.thirstDrainMultiplier = 1.75;
        a.radOxygen = 4;
        a.infectionBar.setVisible(true);
        a.radiationBar.setVisible(true);
        r.data.suspend(on);
        expect(a.thirstBar.getPlayers().isEmpty() && a.infectionBar.getPlayers().isEmpty() && a.radiationBar.getPlayers().isEmpty(), "suspending takes every bar off the screen");
        expect(!a.infectionBar.isVisible() && !a.radiationBar.isVisible(), "and hides the ones that only show sometimes");
        expect(a.hypothermiaStage == 0 && a.heatStage == 0 && a.heatExposureGameTicks == 0 && a.thirstDrainMultiplier == 1.0, "cold, heat and faster thirst are cleared");
        expect(a.radOxygen == 20.0, "radiation oxygen is full again");
        expect(a.thirst == 100, "thirst is kept");

        r.data.resume(on);
        expect(a.thirstBar.getPlayers().contains(on) && a.infectionBar.getPlayers().contains(on), "resuming puts the bars back");
        r.data.resume(on);
        expect(a.thirstBar.getPlayers().size() == 1, "and not twice");

        // players with no state yet
        Who nobody = r.join("Nobody");
        r.data.suspend(nobody);
        r.data.resume(nobody);
        expect(r.data.peek(nobody) == null, "suspending or resuming a player with no state makes none");
    }

    // ------------------------------------------------------------------ catching and spreading

    static void infections() {
        clearServer();
        Rig r = new Rig();
        r.config.chanceMultiplier = 1000.0; // every roll succeeds, so "never" below means never
        Who on = r.join("On");
        Who off = r.join("Off");
        off.hide("nexussurvival");

        r.diseases.infect(off, Disease.FEVER_ROT);
        expect(r.diseases.getInfection(off) == null, "infect does nothing to a player with it off (a vial, an admin give, a bite, the lot)");
        expect(off.messages.isEmpty(), "and says nothing");
        r.diseases.infect(on, Disease.FEVER_ROT);
        expect(r.diseases.getInfection(on) == Disease.FEVER_ROT, "a player with it on is infected as before");

        // person to person
        r.seconds(30);
        expect(r.diseases.getInfection(off) == null, "contagion does not reach a player with it off");
        expect(off.messages.isEmpty(), "and they are told nothing: " + off.messages);
        Who also = r.join("AlsoOn");
        r.seconds(30);
        expect(r.diseases.getInfection(also) != null, "but it does reach one with it on, so the test can tell");
        expect(on.messages.stream().noneMatch(m -> m.contains("Off caught")), "the sick player is not told the off player caught it");

        // a plague site
        clearServer();
        Rig s = new Rig();
        s.config.chanceMultiplier = 1000.0;
        Who dead = s.join("Dead");
        Who ghost = s.join("Ghost");
        ghost.hide("nexussurvival");
        s.diseases.markPlagueSite(dead.getLocation(), Disease.FEVER_ROT);
        s.seconds(5);
        expect(s.diseases.getInfection(ghost) == null, "standing at a plague site does nothing to a player with it off");
        expect(ghost.messages.isEmpty(), "and no 'remains made you sick' message: " + ghost.messages);
        expect(s.diseases.getInfection(dead) != null && dead.messages.stream().anyMatch(m -> m.contains("remains")), "a player with it on is made sick there");

        // switching off while sick
        clearServer();
        Rig t = new Rig();
        Who sick = t.join("Sick");
        t.diseases.infect(sick, Disease.RATTLING_COUGH);
        SurvivalPlayerData d = t.data.get(sick);
        d.infectionSeverity = 3;
        sick.setGlowing(true);
        d.infectionBar.setVisible(true);
        sick.hide("nexussurvival");
        t.second();
        expect(t.diseases.getInfection(sick) == null && d.infectionSeverity == 0, "switching it off cures the illness");
        expect(!d.infectionBar.isVisible(), "and hides its bar");
        expect(!sick.isGlowing(), "a critical illness stops glowing");
        expect(!t.diseases.isImmune(sick), "no immunity window is granted (this was not a cure)");
        t.diseases.dropInfection(sick);
        t.diseases.dropInfection(t.join("Fresh"));
    }

    // ------------------------------------------------------------------ dying of a disease

    static void dying() {
        clearServer();
        Rig r = new Rig();
        r.config.chanceMultiplier = 1000.0;
        Who victim = r.join("Victim");
        r.diseases.infect(victim, Disease.FEVER_ROT);
        SurvivalPlayerData d = r.data.get(victim);
        d.infectionSeverity = 3;
        d.infectionBar.setVisible(true);
        victim.setGlowing(true);
        victim.stubSetLocation(new org.bukkit.Location(WORLD, 50, 64, 50));
        expect(r.diseases.onDeath(victim) == Disease.FEVER_ROT, "dying sick reports the illness");
        expect(r.diseases.getInfection(victim) == null && d.infectionSeverity == 0, "and the player is not sick any more");
        expect(!d.infectionBar.isVisible() && !victim.isGlowing(), "the bar is gone and the glow stops");
        expect(!r.diseases.isImmune(victim), "no immunity window: it was not a cure");
        expect(r.diseases.getRecentPlagueDeaths().size() == 1 && r.diseases.getRecentPlagueDeaths().get(0).startsWith("Victim"), "the death is still recorded");

        // after respawning (elsewhere) they stay healthy through the following seconds, with contagion all around
        victim.stubSetLocation(new org.bukkit.Location(WORLD, 0, 64, 0));
        r.seconds(30);
        expect(r.diseases.getInfection(victim) == null, "nothing re-infects them back at spawn");
        // but the site where they fell still catches someone who lingers there
        Who passer = r.join("Passer");
        passer.stubSetLocation(new org.bukkit.Location(WORLD, 50, 64, 50));
        r.seconds(5);
        expect(r.diseases.getInfection(passer) == Disease.FEVER_ROT, "the plague site where they fell is still dangerous");

        // not sick: nothing happens, nothing is recorded
        Who well = r.join("Well");
        expect(r.diseases.onDeath(well) == null, "dying healthy changes nothing");
        expect(r.diseases.getRecentPlagueDeaths().size() == 1, "and adds no record");
        expect(r.diseases.onDeath(r.join("Newcomer")) == null, "a player with no state yet is fine");

        // the switch
        clearServer();
        Rig s = new Rig();
        s.config.clearOnDeath = false;
        Who stubborn = s.join("Stubborn");
        s.diseases.infect(stubborn, Disease.FEVER_ROT);
        expect(s.diseases.onDeath(stubborn) == Disease.FEVER_ROT && s.diseases.getInfection(stubborn) == Disease.FEVER_ROT,
                "with clear-on-death off the illness is carried through death");
    }

    // ------------------------------------------------------------------ the once-a-second pass

    static void theSecond() {
        clearServer();
        Rig r = new Rig();
        Who on = r.join("On");
        Who off = r.join("Off");
        off.hide("nexussurvival");
        r.data.get(on).thirst = 200;
        SurvivalPlayerData offData = r.data.get(off);
        offData.thirst = 200;
        r.seconds(100);
        expect(r.data.get(on).thirst < 200, "thirst falls for a player with it on (" + r.data.get(on).thirst + ")");
        expect(offData.thirst == 200, "and does not for a player with it off (" + offData.thirst + ")");
        expect(offData.thirstBar.getPlayers().isEmpty(), "their bar stays off their screen");
        expect(offData.thirstBar.getTitle().contains("267"), "and is not redrawn");

        // dehydration damage
        r.data.get(on).thirst = 0;
        offData.thirst = 0;
        int before = on.damageCalls;
        r.seconds(12);
        expect(on.damageCalls > before, "a dry player with it on takes damage");
        expect(off.damageCalls == 0, "a dry player with it off does not (" + off.damageCalls + ")");

        // climate and radiation counters do not move
        offData.hypothermiaStage = 0;
        r.seconds(20);
        expect(offData.hypothermiaTickCounter == 0 && offData.heatTickCounter == 0 && offData.mildWeatherTickCounter == 0, "climate counters stay still");
        expect(offData.radTickCounter == 0 && offData.hygieneTickCounter == 0 && offData.symptomTickCounter == 0, "so do radiation, hygiene and illness counters");
        expect(off.messages.isEmpty(), "and nothing is said to them: " + off.messages);

        // the player with it on is unaffected by all of this
        expect(r.data.get(on).thirstBar.getPlayers().contains(on), "the player with it on keeps their bars");
    }

    // ------------------------------------------------------------------ switching mid-game

    static void switching() {
        clearServer();
        Rig r = new Rig();
        Who p = r.join("Pat");
        r.second();
        SurvivalPlayerData d = r.data.peek(p);
        expect(d != null && d.thirstBar.getPlayers().contains(p), "playing normally: bar shown");
        d.hypothermiaStage = 2;
        d.thirstDrainMultiplier = 1.75;
        double thirstBefore = d.thirst;

        p.hide("nexussurvival");
        r.second();
        expect(d.thirstBar.getPlayers().isEmpty() && d.hypothermiaStage == 0 && d.thirstDrainMultiplier == 1.0, "switching it off clears the bar and the cold at once");
        r.seconds(60);
        expect(d.thirst == thirstBefore, "and thirst holds still while it is off (" + d.thirst + " vs " + thirstBefore + ")");

        p.hide(null);
        r.second();
        expect(d.thirstBar.getPlayers().contains(p) && d.infectionBar.getPlayers().contains(p), "switching it back on shows the bars again");
        r.seconds(60);
        expect(d.thirst < thirstBefore, "and thirst runs down again (" + d.thirst + ")");

        // with the setting off nobody can hide it
        Who q = r.join("Quinn");
        q.hide("nexussurvival");
        r.config.respect = false;
        r.second();
        expect(r.data.get(q).thirstBar.getPlayers().contains(q), "path.respect-nexuspath: false puts the bars back for someone who chose it off");
    }

    // ------------------------------------------------------------------ robustness

    static void broken() {
        clearServer();
        Rig r = new Rig();
        Who bad = r.join("Bad");
        Who good = r.join("Good");
        bad.brokenData = true;
        r.data.get(good).thirst = 200;
        r.second();
        expect(r.logged.stream().anyMatch(m -> m.contains("could not read Bad")), "an unreadable choice is logged");
        expect(r.ticker.appliesTo(bad), "and treated as on");
        expect(r.data.get(good).thirst <= 200, "other players carry on");

        // a step that throws does not stop the others or other players
        clearServer();
        Rig s = new Rig();
        Who a = s.join("A");
        Who b = s.join("B");
        List<String> ran = new ArrayList<>();
        Map<String, Consumer<Player>> steps = new LinkedHashMap<>();
        steps.put("first", p -> ran.add("first-" + p.getName()));
        steps.put("breaks", p -> {
            throw new IllegalStateException("boom");
        });
        steps.put("last", p -> ran.add("last-" + p.getName()));
        s.ticker.tickPlayers(List.of(a, b), steps);
        expect(ran.equals(List.of("first-A", "last-A", "first-B", "last-B")), "a failing step is skipped, the rest still run for everyone: " + ran);
        expect(s.logged.stream().filter(m -> m.contains("'breaks'")).count() == 2, "and is logged each time");

        Who c = s.join("C");
        c.hide("nexussurvival");
        ran.clear();
        s.ticker.tickPlayers(List.of(a, c), steps);
        expect(ran.equals(List.of("first-A", "last-A")), "a player with it off runs no step at all: " + ran);
    }
}
