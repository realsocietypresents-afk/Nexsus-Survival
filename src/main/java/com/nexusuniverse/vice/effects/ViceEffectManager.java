package com.nexusuniverse.vice.effects;

import com.nexusuniverse.vice.ViceConfig;
import com.nexusuniverse.vice.ViceDataManager;
import com.nexusuniverse.vice.VicePlayerData;
import com.nexusuniverse.vice.integration.MoralityMessengerBridge;
import com.nexusuniverse.vice.substances.Category;
import com.nexusuniverse.vice.substances.Substance;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

/**
 * Every substance and both alcohol tiers run through this one engine --
 * effects are driven entirely by Category (DEPRESSANT/STIMULANT/
 * HALLUCINOGEN/MELLOW) and how far the current dose is past that
 * substance's own overdose threshold (a "dose ratio"), not by
 * bespoke per-substance code. Adding a 9th substance later means one
 * new Substance enum entry -- this file doesn't change.
 *
 * Tiers, by dose ratio (current dose / that substance's overdose threshold):
 *   below 0.25  -- nothing yet
 *   0.25-0.5    -- LIGHT
 *   0.5-0.8     -- HEAVY
 *   0.8-1.0     -- SEVERE (vomiting risk begins)
 *   1.0+        -- OVERDOSE (only substances with hasOverdoseRisk=true actually blackout here)
 */
public class ViceEffectManager {

    private static final double LIGHT_RATIO = 0.25;
    private static final double HEAVY_RATIO = 0.5;
    private static final double SEVERE_RATIO = 0.8;
    private static final double OVERDOSE_RATIO = 1.0;

    private final ViceConfig config;
    private final ViceDataManager viceData;
    private final MoralityMessengerBridge messenger;
    private final Random random = new Random();
    private long currentTick = 0;

    public ViceEffectManager(ViceConfig config, ViceDataManager viceData, MoralityMessengerBridge messenger) {
        this.config = config;
        this.viceData = viceData;
        this.messenger = messenger;
    }

    /** Call ONCE per tick pass, before tickEffects() for each player -- this is the shared clock, not a per-player counter. */
    public void advanceClock() {
        currentTick += 20L * config.tickIntervalSeconds();
    }

    /** Called once per player, once per tick pass: decay, tiered effects, vomit rolls, crash detection, combo checks. */
    public void tickEffects(Player player) {
        VicePlayerData data = viceData.get(player.getUniqueId());

        for (Substance substance : Substance.values()) {
            tickSubstance(player, data, substance);
        }
        tickAlcohol(player, data);
        checkCombos(player, data);
        checkBlackoutRecovery(player, data);
    }

    /** Called on its own, separate interval: deals real damage to anyone currently in an active blackout. */
    public void tickBlackoutDamage(Player player) {
        VicePlayerData data = viceData.get(player.getUniqueId());
        if (!data.isInBlackout()) return;
        if (player.isDead()) return;

        player.damage(config.blackoutDamagePerPulse());
        messenger.announce(player, "vice.overdose_blackout", "You're overdosing and taking damage.",
                "Check into rehab (/vice rehab) to clear your system, or ask someone to help you wait it out somewhere safe.");
        int duration = 20 * (config.blackoutPulseIntervalSeconds() + 2);
        applyOverdoseSymptoms(player, data, duration);
    }

    /**
     * What an active overdose actually LOOKS like now depends on what's overdosing, not one
     * blanket blindness/weakness/slowness pulse for everything. Picks whichever active dose is
     * currently furthest past ITS OWN overdose threshold (substance or alcohol) and applies that
     * category's own symptom profile -- a depressant overdose (near-unconscious, barely moving)
     * reads very differently in play from a stimulant one (racing, jittery, burning through food)
     * or a dissociative one (heavy disorientation, lighter on raw weakness).
     */
    private void applyOverdoseSymptoms(Player player, VicePlayerData data, int duration) {
        Category worstCategory = null;
        double worstRatio = 0;
        for (Substance substance : Substance.values()) {
            double dose = data.substanceDose(substance);
            if (dose <= 0) continue;
            double ratio = dose / config.overdoseThreshold(substance);
            if (ratio > worstRatio) {
                worstRatio = ratio;
                worstCategory = substance.category();
            }
        }

        double alcoholRatio = data.alcoholDose() / config.alcoholBlackoutThreshold();
        if (worstCategory == null || alcoholRatio > worstRatio) {
            // alcohol poisoning profile -- classic drunken shutdown: nausea, heavy slowness, fading vision
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, 2, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 3, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 1, true, true));
            return;
        }

        switch (worstCategory) {
            case DEPRESSANT -> { // respiratory-depression profile -- barely able to move, near-unconscious
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 4, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 3, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, duration, 0, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 1, true, true));
            }
            case STIMULANT -> { // racing-heart profile -- jittery and can't hold still, burning through food fast
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 2, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, 1, true, true));
                jitter(player, 1.0);
                if (player.getFoodLevel() > 2) player.setFoodLevel(player.getFoodLevel() - 1);
            }
            case DISSOCIATIVE -> { // detached-from-your-body profile -- heavy on disorientation, lighter on raw weakness
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, duration, 0, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, 2, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 2, true, true));
            }
            case EUPHORIC -> { // Rapturine is the only euphoric with real overdose risk -- the glow collapsing into something dangerous
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 3, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 2, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 1, true, true));
            }
            case HALLUCINOGEN, MELLOW, PERFORMANCE -> {
                // none of these categories currently have any hasOverdoseRisk=true substance, so
                // this branch shouldn't be reachable in practice -- kept as a safe generic fallback
                // rather than assuming that never changes
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 1, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 2, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 3, true, true));
            }
        }
    }

    private void tickSubstance(Player player, VicePlayerData data, Substance substance) {
        double dose = data.substanceDose(substance);
        if (dose <= 0) {
            checkForCrash(player, data, substance);
            return;
        }

        double decayed = Math.max(0, dose - config.decayPerTick());
        data.setSubstanceDose(substance, decayed);

        if (decayed <= 0) {
            checkForCrash(player, data, substance);
            return;
        }

        double threshold = config.overdoseThreshold(substance);
        double ratio = decayed / threshold;

        applyCategoryEffects(player, substance.category(), ratio);
        rollVomitCheck(player, data, ratio);

        if (config.hasOverdoseRisk(substance) && ratio >= OVERDOSE_RATIO) {
            enterBlackout(player, data, config.displayName(substance) + " overdose");
        }
    }

    private void tickAlcohol(Player player, VicePlayerData data) {
        double dose = data.alcoholDose();
        if (dose <= 0) {
            checkForAlcoholCrash(player, data);
            return;
        }

        double decayed = Math.max(0, dose - config.decayPerTick());
        data.setAlcoholDose(decayed);

        if (decayed <= 0) {
            checkForAlcoholCrash(player, data);
            return;
        }

        double ratio = decayed / config.alcoholBlackoutThreshold();
        applyAlcoholEffects(player, ratio);
        rollVomitCheck(player, data, ratio);

        if (ratio >= OVERDOSE_RATIO) {
            enterBlackout(player, data, "alcohol poisoning");
        }
    }

    // --- Category effect profiles ---

    private void applyCategoryEffects(Player player, Category category, double ratio) {
        switch (category) {
            case DEPRESSANT -> applyDepressantEffects(player, ratio);
            case STIMULANT -> applyStimulantEffects(player, ratio);
            case HALLUCINOGEN -> applyHallucinogenEffects(player, ratio);
            case MELLOW -> applyMellowEffects(player, ratio);
            case DISSOCIATIVE -> applyDissociativeEffects(player, ratio);
            case PERFORMANCE -> applyPerformanceEffects(player, ratio);
            case EUPHORIC -> applyEuphoricEffects(player, ratio);
        }
    }

    private void applyDepressantEffects(Player player, double ratio) {
        if (ratio < LIGHT_RATIO) return;
        int amp = ratio >= OVERDOSE_RATIO ? 3 : ratio >= SEVERE_RATIO ? 2 : ratio >= HEAVY_RATIO ? 1 : 0;
        int duration = effectDuration();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, amp, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, Math.max(0, amp - 1), true, true));
        if (ratio >= SEVERE_RATIO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, 0, true, true));
        }
    }

    private void applyStimulantEffects(Player player, double ratio) {
        if (ratio < LIGHT_RATIO) return;
        int amp = ratio >= SEVERE_RATIO ? 2 : ratio >= HEAVY_RATIO ? 1 : 0;
        int duration = effectDuration();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amp, true, true));
        if (ratio >= HEAVY_RATIO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration, Math.max(0, amp - 1), true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 0, true, true));
        }
        if (ratio >= SEVERE_RATIO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, 0, true, true));
            if (player.getFoodLevel() > 4) player.setFoodLevel(player.getFoodLevel() - 1); // racing heart burns through your energy
        }
        if (ratio >= OVERDOSE_RATIO) {
            jitter(player, 0.6);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, true, true));
        }
    }

    private void applyHallucinogenEffects(Player player, double ratio) {
        if (ratio < LIGHT_RATIO) return;
        int duration = effectDuration();
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, ratio >= HEAVY_RATIO ? 1 : 0, true, true));
        if (ratio >= HEAVY_RATIO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 15, 0, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 0, true, true));
        }
        if (ratio >= SEVERE_RATIO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, true, true));
            jitter(player, 0.3);
        }
    }

    private void applyMellowEffects(Player player, double ratio) {
        if (ratio < HEAVY_RATIO) return; // deliberately mild -- Herbalis barely does anything until you've had a lot
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, effectDuration(), 0, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20, 0, true, true));
    }

    /** The "detached from your own body" profile -- heavy on vision distortion, light on movement penalty. */
    private void applyDissociativeEffects(Player player, double ratio) {
        if (ratio < LIGHT_RATIO) return;
        int duration = effectDuration();
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, ratio >= HEAVY_RATIO ? 1 : 0, true, true));
        if (ratio >= HEAVY_RATIO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 1, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 25, 0, true, true));
        }
        if (ratio >= SEVERE_RATIO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, duration, 0, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, true, true));
        }
        if (ratio >= OVERDOSE_RATIO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 2, true, true));
        }
    }

    /** Gains now, consequences later -- real strength/regen while active, nothing bad until the crash. */
    private void applyPerformanceEffects(Player player, double ratio) {
        if (ratio < LIGHT_RATIO) return;
        int duration = effectDuration();
        int amp = ratio >= SEVERE_RATIO ? 2 : ratio >= HEAVY_RATIO ? 1 : 0;
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, amp, true, true));
        if (ratio >= HEAVY_RATIO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 0, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0, true, true));
        }
        if (ratio >= SEVERE_RATIO && player.getFoodLevel() > 4) {
            player.setFoodLevel(player.getFoodLevel() - 1); // the body is burning through itself to keep this up
        }
    }

    /** Warm, glowing, floaty -- the "everything is wonderful" high. Real short-term upside, no real damage risk except Rapturine specifically. */
    private void applyEuphoricEffects(Player player, double ratio) {
        if (ratio < LIGHT_RATIO) return;
        int duration = effectDuration();
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0, true, true));
        if (ratio >= HEAVY_RATIO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, 0, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, true));
        }
        if (ratio >= SEVERE_RATIO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 30, 0, true, true)); // floaty happiness, brief pulses
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20, 0, true, true)); // sensory overload at the top of the high
        }
        if (ratio >= OVERDOSE_RATIO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 2, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, true, true));
        }
    }

    private void applyAlcoholEffects(Player player, double ratio) {
        if (ratio < LIGHT_RATIO) return;
        int amp = ratio >= OVERDOSE_RATIO ? 3 : ratio >= SEVERE_RATIO ? 2 : ratio >= HEAVY_RATIO ? 1 : 0;
        int duration = effectDuration();
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, amp, true, true)); // the drunken screen-wobble
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, Math.max(0, amp - 1), true, true));
        if (ratio >= HEAVY_RATIO) {
            stumble(player, ratio); // the real, involuntary "character moves on its own" lurch -- Nausea alone never actually moves the player
        }
        if (ratio >= SEVERE_RATIO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 15, 0, true, true));
        }
    }

    /**
     * The literal "moving on their own, like they're stumbling" ask. Each effect-tick pass while
     * heavily drunk there's a chance (higher and stronger the drunker they are) of a real
     * involuntary step -- a velocity shove in a direction rotated a random amount off whichever
     * way the player is actually facing, so it reads as a stumble forward/sideways rather than a
     * random teleport-feeling yank. This is separate from jitter() (a small instantaneous nudge
     * used elsewhere for stimulant/hallucinogen jitteriness) -- a stumble is bigger and biased by
     * facing direction, meant to look like an actual missed step.
     */
    private void stumble(Player player, double ratio) {
        double chance = ratio >= OVERDOSE_RATIO ? 0.9 : ratio >= SEVERE_RATIO ? 0.6 : 0.3;
        if (random.nextDouble() >= chance) return;

        double strength = 0.35 + (ratio >= OVERDOSE_RATIO ? 0.35 : ratio >= SEVERE_RATIO ? 0.2 : 0.0);
        double angleOffset = (random.nextDouble() - 0.5) * Math.PI; // up to +-90 degrees off where they're actually facing
        Vector stumbleDir = rotateAroundY(player.getLocation().getDirection(), angleOffset).multiply(strength);
        stumbleDir.setY(0.1);
        player.setVelocity(player.getVelocity().add(stumbleDir));
    }

    private Vector rotateAroundY(Vector v, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        return new Vector(v.getX() * cos - v.getZ() * sin, v.getY(), v.getX() * sin + v.getZ() * cos);
    }

    private void jitter(Player player, double strength) {
        double dx = (random.nextDouble() - 0.5) * strength;
        double dz = (random.nextDouble() - 0.5) * strength;
        player.setVelocity(player.getVelocity().add(new Vector(dx, 0.05, dz)));
    }

    private int effectDuration() {
        return 20 * (config.tickIntervalSeconds() + 2); // outlasts the tick interval so effects don't flicker between applications
    }

    // --- Vomiting ---

    private void rollVomitCheck(Player player, VicePlayerData data, double ratio) {
        double chance;
        if (ratio >= OVERDOSE_RATIO) chance = config.vomitOverdoseChance();
        else if (ratio >= SEVERE_RATIO) chance = config.vomitSevereChance();
        else return;

        long cooldownTicks = 20L * config.vomitCooldownSeconds();
        if (currentTick - data.lastVomitTick() < cooldownTicks) return;

        if (random.nextDouble() < chance) {
            triggerVomit(player);
            data.setLastVomitTick(currentTick);
        }
    }

    private void triggerVomit(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 1, true, true));
        if (player.getFoodLevel() > 2) player.setFoodLevel(player.getFoodLevel() - 2);
        player.setSaturation(Math.max(0, player.getSaturation() - 2f));

        Location mouth = player.getEyeLocation().subtract(0, 0.25, 0);
        player.getWorld().spawnParticle(Particle.DUST, mouth, 16, 0.3, 0.2, 0.3,
                new Particle.DustOptions(Color.fromRGB(107, 87, 40), 1.2f));
        player.getWorld().playSound(mouth, Sound.ENTITY_PLAYER_BURP, 1.0f, 0.6f);
        spawnVomitChunks(player, mouth);

        player.sendMessage("§2You feel violently ill and throw up.");
    }

    /** The literal "blocks flying out of your head" bit -- a few real dropped-item entities (mostly dirt), launched outward from the mouth in the direction the player's facing, that land and despawn like any other dropped item. */
    private void spawnVomitChunks(Player player, Location mouth) {
        List<Material> pool = config.vomitBlocks();
        Vector facing = player.getLocation().getDirection();
        int count = 2 + random.nextInt(3); // 2-4 chunks per heave

        for (int i = 0; i < count; i++) {
            Material material = pool.get(random.nextInt(pool.size()));
            Item dropped = player.getWorld().dropItem(mouth, new ItemStack(material));
            dropped.setPickupDelay(200); // ~10s -- long enough it doesn't just get sucked straight back into the player who dropped it

            Vector outward = facing.clone().multiply(0.2 + random.nextDouble() * 0.15);
            outward.setY(0.25 + random.nextDouble() * 0.15);
            outward.add(new Vector((random.nextDouble() - 0.5) * 0.2, 0, (random.nextDouble() - 0.5) * 0.2));
            dropped.setVelocity(outward);
        }
    }

    // --- Overdose / blackout ---

    private void enterBlackout(Player player, VicePlayerData data, String cause) {
        if (data.isInBlackout()) return;
        data.setInBlackout(true);
        viceData.incrementOverdoseCount(player.getUniqueId()); // a lifetime stat, persisted -- see ViceDataManager
        player.sendMessage("§4§lYou've overdosed. §cYour body is shutting down -- this is serious.");
        Bukkit.broadcastMessage("§4§l\u26A0 " + player.getName() + " is overdosing on " + cause + "!");
    }

    private void checkBlackoutRecovery(Player player, VicePlayerData data) {
        if (!data.isInBlackout()) return;
        if (anyDoseAtOrAboveSevere(data)) return;

        data.setInBlackout(false);
        player.sendMessage("§aYou're stabilizing. The overdose has passed.");
    }

    private boolean anyDoseAtOrAboveSevere(VicePlayerData data) {
        for (Substance substance : Substance.values()) {
            double dose = data.substanceDose(substance);
            if (dose <= 0) continue;
            if (dose / config.overdoseThreshold(substance) >= SEVERE_RATIO) return true;
        }
        double alcohol = data.alcoholDose();
        return alcohol > 0 && alcohol / config.alcoholBlackoutThreshold() >= SEVERE_RATIO;
    }

    // --- Crashes / comedowns ---

    private void checkForCrash(Player player, VicePlayerData data, Substance substance) {
        double peak = data.peakSubstanceDose(substance);
        if (peak <= 0) return;

        if (peak / config.overdoseThreshold(substance) < HEAVY_RATIO) {
            data.resetPeak(substance); // never got high enough to owe a comedown
            return;
        }
        applyCrash(player, substance.category());
        data.resetPeak(substance);
    }

    private void checkForAlcoholCrash(Player player, VicePlayerData data) {
        double peak = data.peakAlcoholDose();
        if (peak <= 0) return;

        if (peak / config.alcoholBlackoutThreshold() < HEAVY_RATIO) {
            data.resetAlcoholPeak();
            return;
        }

        int duration = 20 * 45;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 1, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 0, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration, 0, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 10, 0, true, true));
        player.sendMessage("§7A pounding hangover sets in.");
        data.resetAlcoholPeak();
    }

    private void applyCrash(Player player, Category category) {
        int duration = 20 * 30;
        switch (category) {
            case STIMULANT -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 2, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 1, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration, 1, true, true));
                player.sendMessage("§7The crash hits hard. You feel drained.");
            }
            case DEPRESSANT -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration / 2, 0, true, true));
                player.sendMessage("§7You feel groggy as it wears off.");
            }
            case DISSOCIATIVE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, 0, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration / 2, 0, true, true));
                player.sendMessage("§7Reality slowly comes back into focus.");
            }
            case PERFORMANCE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 2, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 1, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration, 2, true, true));
                player.sendMessage("§7Your body is paying for that now. Everything aches.");
            }
            case EUPHORIC -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 1, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration / 2, 0, true, true));
                player.sendMessage("§7The glow fades into something heavier.");
            }
            case HALLUCINOGEN, MELLOW -> player.sendMessage("§7The effects fade.");
        }
    }

    // --- Dangerous combos ---

    /** A depressant substance active alongside alcohol accelerates itself -- the two together are worse than either alone. */
    private void checkCombos(Player player, VicePlayerData data) {
        double alcohol = data.alcoholDose();
        if (alcohol <= 0 || alcohol / config.alcoholBlackoutThreshold() < LIGHT_RATIO) return;

        for (Substance substance : Substance.values()) {
            if (substance.category() != Category.DEPRESSANT) continue;

            double dose = data.substanceDose(substance);
            double threshold = config.overdoseThreshold(substance);
            if (dose <= 0 || dose / threshold < LIGHT_RATIO) continue;

            double bonus = config.comboDepressantAlcoholMultiplier();
            data.setSubstanceDose(substance, dose * (1 + (bonus - 1) * 0.1)); // gentle per-tick nudge, not an instant jump
        }
    }
}
