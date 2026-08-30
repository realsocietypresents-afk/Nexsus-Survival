package com.nexusuniverse.vice.guidebook;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * A real WRITTEN_BOOK covering the whole plugin end to end -- every substance's category/effect
 * profile and exact recipe, every alcohol brand and its recipe, and how overdose/vomiting/rehab/
 * combos work. This is the single source of truth for "how do I craft X" in-game; if a recipe
 * ever changes in ViceRecipes, the matching line here needs to change too (there's no shared data
 * structure between them -- ViceRecipes registers recipes imperatively, this just describes them
 * in prose, so keep both in sync by hand).
 */
public class GuidebookItem {

    public ItemStack create() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("Vice Field Guide");
        meta.setAuthor("Nexus Universe");
        for (String page : BookPaginator.paginate(buildContent())) {
            meta.addPage(page);
        }
        book.setItemMeta(meta);
        return book;
    }

    private List<String> buildContent() {
        List<String> p = new ArrayList<>();

        p.add("VICE FIELD GUIDE");
        p.add("Everything here is invented -- no substance maps to a real drug or real brand.");
        p.add("Every substance and alcohol brand is crafted in a normal crafting table. Ingredients can go anywhere in the grid -- these are all shapeless recipes.");

        p.add("=== STEP 1: CHEMICAL BASE ===");
        p.add("Every substance (not alcohol) starts here. Craft: Glass Bottle + Gunpowder + Redstone + Blaze Powder -> Chemical Base.");
        p.add("Then: Chemical Base + that substance's own ingredients (below) -> the finished substance.");

        p.add("=== DEPRESSANTS ===");
        p.add("Slow you down. Heavy doses risk a real overdose blackout.");
        p.add("Fentinoli: Base + Sugar + Blaze Powder.");
        p.add("Xanaxel: Base + Paper + Redstone.");
        p.add("Opiatrix: Base + Poppy + Sugar.");
        p.add("Morphene: Base + Ghast Tear + Sugar.");

        p.add("=== STIMULANTS ===");
        p.add("Speed and haste, at a cost -- hunger drain, and the hardest ones carry real overdose risk.");
        p.add("Molotine: Base + Blaze Powder + Glowstone Dust.");
        p.add("Cocainium: Base + 2x Snowball + Sugar.");
        p.add("Molly-Q: Base + Pink Dye + Sugar.");
        p.add("Nicotane: Base + Stick + Charcoal.");
        p.add("Caffinex: Base + Cocoa Beans + Sugar.");
        p.add("Cryotine: Base + Quartz + Packed Ice. (Overdose risk.)");
        p.add("Crankstone: Base + Nether Wart + Glowstone Dust. (Overdose risk.)");

        p.add("=== HALLUCINOGENS ===");
        p.add("Vision distortion, no real overdose risk.");
        p.add("Acidrop: Base + Phantom Membrane + Fermented Spider Eye.");
        p.add("Sporeline: Base + Red Mushroom + Brown Mushroom.");
        p.add("Riftsmoke: Base + Crimson Fungus + Fermented Spider Eye.");

        p.add("=== MELLOW ===");
        p.add("Barely does anything until you've had a lot. No real overdose risk.");
        p.add("Herbalis: Base + 2x Wheat.");
        p.add("Driftweed: Base + Kelp + Sugar.");
        p.add("Gummel: Base + Melon Slice + Sugar.");

        p.add("=== DISSOCIATIVES ===");
        p.add("Detached-from-your-body profile. Real overdose risk.");
        p.add("Ketrazine: Base + Prismarine Crystals + Prismarine Shard.");
        p.add("Somnara: Base + Echo Shard + Soul Sand.");
        p.add("Voidcap: Base + Chorus Fruit + Echo Shard.");

        p.add("=== PERFORMANCE ===");
        p.add("Real strength/regen while active, a heavy crash once it wears off. No real overdose risk.");
        p.add("Anabolex: Base + 2x Iron Nugget.");
        p.add("Titanex: Base + Copper Ingot + Iron Nugget.");

        p.add("=== EUPHORICS ===");
        p.add("Warm, glowing, floaty. Rapturine is the one exception with real overdose risk.");
        p.add("Euphorion: Base + Glow Berries + Glowstone Dust.");
        p.add("Blissenta: Base + Honeycomb + Sugar.");
        p.add("Rapturine: Base + Amethyst Shard + Glowstone Dust. (Overdose risk.)");

        p.add("=== ALCOHOL ===");
        p.add("No Chemical Base needed -- just a Glass Bottle plus ingredients. Every brand shares one alcohol dose pool no matter what you mix.");
        p.add("Three qualities per type: bottom-shelf (weakest), standard, top-shelf (strongest). More ingredients = better quality.");

        p.add("-- Beer --");
        p.add("Rustbucket Lager (bottom): Bottle + Wheat.");
        p.add("Hopfield Ale (standard): Bottle + 2x Wheat.");
        p.add("Ironcrest Stout (top): Bottle + 2x Wheat + Cocoa Beans.");

        p.add("-- Wine --");
        p.add("Cellar Jug Red (bottom): Bottle + Sweet Berries.");
        p.add("Highvale Merlot (standard): Bottle + Sweet Berries + Sugar.");
        p.add("Estate Reserve (top): Bottle + Sweet Berries + Sugar + Glowstone Dust.");

        p.add("-- Liquor --");
        p.add("Moonshine Jar (bottom): Bottle + Potato.");
        p.add("Blackrock Whiskey (standard): Bottle + Potato + Sugar.");
        p.add("Gilded Reserve (top): Bottle + Potato + Sugar + Gold Nugget.");

        p.add("=== HOW IT HITS ===");
        p.add("Each dose builds up and decays over time. Past 25% of overdose threshold, effects start. They get worse at 50%, 80%, and 100%+.");
        p.add("Heavy drinking past 50% causes real involuntary stumbling -- your character will lurch on its own, not just a screen wobble.");
        p.add("Past 80% you risk vomiting -- it's not just for show, real items fly out of your mouth.");
        p.add("At 100%+ on anything with real overdose risk, you black out: real damage over time, and symptoms that match what actually overdosed (a depressant blackout looks different from a stimulant one).");
        p.add("Mixing a depressant with alcohol is dangerous -- they accelerate each other.");
        p.add("Coming down hard from something (past 50% at its peak) means a comedown once it fully wears off. Get high, pay for it later.");

        p.add("=== GETTING CLEAN ===");
        p.add("/vice rehab wipes every active dose instantly. It has a real cooldown, so it isn't a free undo button.");
        p.add("/vice status shows everything currently in your system.");

        return p;
    }
}
