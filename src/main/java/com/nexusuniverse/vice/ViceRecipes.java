package com.nexusuniverse.vice;

import com.nexusuniverse.vice.alcohol.AlcoholBrand;
import com.nexusuniverse.vice.alcohol.AlcoholItems;
import com.nexusuniverse.vice.substances.Substance;
import com.nexusuniverse.vice.substances.SubstanceItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * A genuine two-stage crafting chain:
 *
 *   Stage 1 (shared): four common ingredients -> one "Chemical Base",
 *   the precursor every substance needs, regardless of which one
 *   you're actually making.
 *
 *   Stage 2 (per-substance): Chemical Base + that substance's own
 *   signature item + 1-2 more ingredients -> the finished substance.
 *
 * Alcohol stays single-stage (Glass Bottle + 2 ingredients) -- it was
 * never asked to get more elaborate.
 */
public final class ViceRecipes {

    private ViceRecipes() {}

    public static void registerAll(Plugin plugin, SubstanceItems substanceItems, AlcoholItems alcoholItems) {
        ItemStack chemicalBase = createChemicalBase(plugin);
        registerChemicalBaseRecipe(plugin, chemicalBase);

        registerSubstance(plugin, substanceItems, chemicalBase, Substance.FENTINOLI, Material.SUGAR, Material.BLAZE_POWDER);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.XANAXEL, Material.PAPER, Material.REDSTONE);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.OPIATRIX, Material.POPPY, Material.SUGAR);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.MOLOTINE, Material.BLAZE_POWDER, Material.GLOWSTONE_DUST);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.MOLLYQ, Material.PINK_DYE, Material.SUGAR);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.ACIDROP, Material.PHANTOM_MEMBRANE, Material.FERMENTED_SPIDER_EYE);
        registerCocainium(plugin, substanceItems, chemicalBase);
        registerHerbalis(plugin, substanceItems, chemicalBase);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.NICOTANE, Material.STICK, Material.CHARCOAL);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.CAFFINEX, Material.COCOA_BEANS, Material.SUGAR);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.CRYOTINE, Material.QUARTZ, Material.PACKED_ICE);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.SPORELINE, Material.RED_MUSHROOM, Material.BROWN_MUSHROOM);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.KETRAZINE, Material.PRISMARINE_CRYSTALS, Material.PRISMARINE_SHARD);
        registerAnabolex(plugin, substanceItems, chemicalBase);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.DRIFTWEED, Material.KELP, Material.SUGAR);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.SOMNARA, Material.ECHO_SHARD, Material.SOUL_SAND);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.TITANEX, Material.COPPER_INGOT, Material.IRON_NUGGET);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.EUPHORION, Material.GLOW_BERRIES, Material.GLOWSTONE_DUST);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.BLISSENTA, Material.HONEYCOMB, Material.SUGAR);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.RAPTURINE, Material.AMETHYST_SHARD, Material.GLOWSTONE_DUST);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.MORPHENE, Material.GHAST_TEAR, Material.SUGAR);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.CRANKSTONE, Material.NETHER_WART, Material.GLOWSTONE_DUST);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.RIFTSMOKE, Material.CRIMSON_FUNGUS, Material.FERMENTED_SPIDER_EYE);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.VOIDCAP, Material.CHORUS_FRUIT, Material.ECHO_SHARD);
        registerSubstance(plugin, substanceItems, chemicalBase, Substance.GUMMEL, Material.MELON_SLICE, Material.SUGAR);

        registerAlcoholBrand(plugin, alcoholItems, AlcoholBrand.RUSTBUCKET_LAGER, Material.WHEAT);
        registerAlcoholBrand(plugin, alcoholItems, AlcoholBrand.HOPFIELD_ALE, Material.WHEAT, Material.WHEAT);
        registerAlcoholBrand(plugin, alcoholItems, AlcoholBrand.IRONCREST_STOUT, Material.WHEAT, Material.WHEAT, Material.COCOA_BEANS);

        registerAlcoholBrand(plugin, alcoholItems, AlcoholBrand.CELLAR_JUG_RED, Material.SWEET_BERRIES);
        registerAlcoholBrand(plugin, alcoholItems, AlcoholBrand.HIGHVALE_MERLOT, Material.SWEET_BERRIES, Material.SUGAR);
        registerAlcoholBrand(plugin, alcoholItems, AlcoholBrand.ESTATE_RESERVE, Material.SWEET_BERRIES, Material.SUGAR, Material.GLOWSTONE_DUST);

        registerAlcoholBrand(plugin, alcoholItems, AlcoholBrand.MOONSHINE_JAR, Material.POTATO);
        registerAlcoholBrand(plugin, alcoholItems, AlcoholBrand.BLACKROCK_WHISKEY, Material.POTATO, Material.SUGAR);
        registerAlcoholBrand(plugin, alcoholItems, AlcoholBrand.GILDED_RESERVE, Material.POTATO, Material.SUGAR, Material.GOLD_NUGGET);
    }

    private static ItemStack createChemicalBase(Plugin plugin) {
        NamespacedKey tagKey = new NamespacedKey(plugin, "vice_chemical_base_item");
        ItemStack item = new ItemStack(Material.GLASS_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7Chemical Base");
        meta.setLore(List.of("§8An unfinished precursor.", "§8Combine with the right ingredients to finish something."));
        meta.getPersistentDataContainer().set(tagKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private static void registerChemicalBaseRecipe(Plugin plugin, ItemStack chemicalBase) {
        NamespacedKey key = new NamespacedKey(plugin, "vice_chemical_base_recipe");
        ShapelessRecipe recipe = new ShapelessRecipe(key, chemicalBase);
        recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.GLASS_BOTTLE));
        recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.GUNPOWDER));
        recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.REDSTONE));
        recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.BLAZE_POWDER));
        Bukkit.addRecipe(recipe);
    }

    private static void registerSubstance(Plugin plugin, SubstanceItems items, ItemStack chemicalBase, Substance substance, Material a, Material b) {
        NamespacedKey key = new NamespacedKey(plugin, "vice_" + substance.configKey());
        ItemStack result = items.create(substance);

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(new RecipeChoice.ExactChoice(chemicalBase));
        recipe.addIngredient(new RecipeChoice.MaterialChoice(a));
        recipe.addIngredient(new RecipeChoice.MaterialChoice(b));
        Bukkit.addRecipe(recipe);
    }

    /** Cocainium needs two snowballs, not one -- "use all the [snowballs]," as requested. */
    private static void registerCocainium(Plugin plugin, SubstanceItems items, ItemStack chemicalBase) {
        NamespacedKey key = new NamespacedKey(plugin, "vice_cocainium");
        ItemStack result = items.create(Substance.COCAINIUM);

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(new RecipeChoice.ExactChoice(chemicalBase));
        recipe.addIngredient(2, new ItemStack(Material.SNOWBALL));
        recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.SUGAR));
        Bukkit.addRecipe(recipe);
    }

    /** Herbalis wants a proper bundle, not a single stem -- two wheat. */
    private static void registerHerbalis(Plugin plugin, SubstanceItems items, ItemStack chemicalBase) {
        NamespacedKey key = new NamespacedKey(plugin, "vice_herbalis");
        ItemStack result = items.create(Substance.HERBALIS);

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(new RecipeChoice.ExactChoice(chemicalBase));
        recipe.addIngredient(2, new ItemStack(Material.WHEAT));
        Bukkit.addRecipe(recipe);
    }

    /** Pumping iron, literally -- two iron nuggets. */
    private static void registerAnabolex(Plugin plugin, SubstanceItems items, ItemStack chemicalBase) {
        NamespacedKey key = new NamespacedKey(plugin, "vice_anabolex");
        ItemStack result = items.create(Substance.ANABOLEX);

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(new RecipeChoice.ExactChoice(chemicalBase));
        recipe.addIngredient(2, new ItemStack(Material.IRON_NUGGET));
        Bukkit.addRecipe(recipe);
    }

    private static void registerAlcoholBrand(Plugin plugin, AlcoholItems items, AlcoholBrand brand, Material... ingredients) {
        NamespacedKey key = new NamespacedKey(plugin, "vice_alcohol_" + brand.configKey());
        ItemStack result = items.create(brand);

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.GLASS_BOTTLE));
        for (Material ingredient : ingredients) {
            recipe.addIngredient(new RecipeChoice.MaterialChoice(ingredient));
        }
        Bukkit.addRecipe(recipe);
    }
}
