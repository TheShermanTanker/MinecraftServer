package net.minecraft.world.item.alchemy;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;

public class PotionBrewer {
    public static final int BREWING_TIME_SECONDS = 20;
    private static final List<PotionBrewer.PredicatedCombination<PotionRegistry>> POTION_MIXES = Lists.newArrayList();
    private static final List<PotionBrewer.PredicatedCombination<Item>> CONTAINER_MIXES = Lists.newArrayList();
    private static final List<RecipeItemStack> ALLOWED_CONTAINERS = Lists.newArrayList();
    private static final Predicate<ItemStack> ALLOWED_CONTAINER = (stack) -> {
        for(RecipeItemStack ingredient : ALLOWED_CONTAINERS) {
            if (ingredient.test(stack)) {
                return true;
            }
        }

        return false;
    };

    public static boolean isIngredient(ItemStack stack) {
        return isContainerIngredient(stack) || isPotionIngredient(stack);
    }

    protected static boolean isContainerIngredient(ItemStack stack) {
        int i = 0;

        for(int j = CONTAINER_MIXES.size(); i < j; ++i) {
            if ((CONTAINER_MIXES.get(i)).ingredient.test(stack)) {
                return true;
            }
        }

        return false;
    }

    protected static boolean isPotionIngredient(ItemStack stack) {
        int i = 0;

        for(int j = POTION_MIXES.size(); i < j; ++i) {
            if ((POTION_MIXES.get(i)).ingredient.test(stack)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isBrewablePotion(PotionRegistry potion) {
        int i = 0;

        for(int j = POTION_MIXES.size(); i < j; ++i) {
            if ((POTION_MIXES.get(i)).to == potion) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasMix(ItemStack input, ItemStack ingredient) {
        if (!ALLOWED_CONTAINER.test(input)) {
            return false;
        } else {
            return hasContainerMix(input, ingredient) || hasPotionMix(input, ingredient);
        }
    }

    protected static boolean hasContainerMix(ItemStack input, ItemStack ingredient) {
        Item item = input.getItem();
        int i = 0;

        for(int j = CONTAINER_MIXES.size(); i < j; ++i) {
            PotionBrewer.PredicatedCombination<Item> mix = CONTAINER_MIXES.get(i);
            if (mix.from == item && mix.ingredient.test(ingredient)) {
                return true;
            }
        }

        return false;
    }

    protected static boolean hasPotionMix(ItemStack input, ItemStack ingredient) {
        PotionRegistry potion = PotionUtil.getPotion(input);
        int i = 0;

        for(int j = POTION_MIXES.size(); i < j; ++i) {
            PotionBrewer.PredicatedCombination<PotionRegistry> mix = POTION_MIXES.get(i);
            if (mix.from == potion && mix.ingredient.test(ingredient)) {
                return true;
            }
        }

        return false;
    }

    public static ItemStack mix(ItemStack ingredient, ItemStack input) {
        if (!input.isEmpty()) {
            PotionRegistry potion = PotionUtil.getPotion(input);
            Item item = input.getItem();
            int i = 0;

            for(int j = CONTAINER_MIXES.size(); i < j; ++i) {
                PotionBrewer.PredicatedCombination<Item> mix = CONTAINER_MIXES.get(i);
                if (mix.from == item && mix.ingredient.test(ingredient)) {
                    return PotionUtil.setPotion(new ItemStack(mix.to), potion);
                }
            }

            i = 0;

            for(int l = POTION_MIXES.size(); i < l; ++i) {
                PotionBrewer.PredicatedCombination<PotionRegistry> mix2 = POTION_MIXES.get(i);
                if (mix2.from == potion && mix2.ingredient.test(ingredient)) {
                    return PotionUtil.setPotion(new ItemStack(item), mix2.to);
                }
            }
        }

        return input;
    }

    public static void bootStrap() {
        addContainer(Items.POTION);
        addContainer(Items.SPLASH_POTION);
        addContainer(Items.LINGERING_POTION);
        addContainerRecipe(Items.POTION, Items.GUNPOWDER, Items.SPLASH_POTION);
        addContainerRecipe(Items.SPLASH_POTION, Items.DRAGON_BREATH, Items.LINGERING_POTION);
        addMix(Potions.WATER, Items.GLISTERING_MELON_SLICE, Potions.MUNDANE);
        addMix(Potions.WATER, Items.GHAST_TEAR, Potions.MUNDANE);
        addMix(Potions.WATER, Items.RABBIT_FOOT, Potions.MUNDANE);
        addMix(Potions.WATER, Items.BLAZE_POWDER, Potions.MUNDANE);
        addMix(Potions.WATER, Items.SPIDER_EYE, Potions.MUNDANE);
        addMix(Potions.WATER, Items.SUGAR, Potions.MUNDANE);
        addMix(Potions.WATER, Items.MAGMA_CREAM, Potions.MUNDANE);
        addMix(Potions.WATER, Items.GLOWSTONE_DUST, Potions.THICK);
        addMix(Potions.WATER, Items.REDSTONE, Potions.MUNDANE);
        addMix(Potions.WATER, Items.NETHER_WART, Potions.AWKWARD);
        addMix(Potions.AWKWARD, Items.GOLDEN_CARROT, Potions.NIGHT_VISION);
        addMix(Potions.NIGHT_VISION, Items.REDSTONE, Potions.LONG_NIGHT_VISION);
        addMix(Potions.NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.INVISIBILITY);
        addMix(Potions.LONG_NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.LONG_INVISIBILITY);
        addMix(Potions.INVISIBILITY, Items.REDSTONE, Potions.LONG_INVISIBILITY);
        addMix(Potions.AWKWARD, Items.MAGMA_CREAM, Potions.FIRE_RESISTANCE);
        addMix(Potions.FIRE_RESISTANCE, Items.REDSTONE, Potions.LONG_FIRE_RESISTANCE);
        addMix(Potions.AWKWARD, Items.RABBIT_FOOT, Potions.LEAPING);
        addMix(Potions.LEAPING, Items.REDSTONE, Potions.LONG_LEAPING);
        addMix(Potions.LEAPING, Items.GLOWSTONE_DUST, Potions.STRONG_LEAPING);
        addMix(Potions.LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
        addMix(Potions.LONG_LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
        addMix(Potions.SLOWNESS, Items.REDSTONE, Potions.LONG_SLOWNESS);
        addMix(Potions.SLOWNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SLOWNESS);
        addMix(Potions.AWKWARD, Items.TURTLE_HELMET, Potions.TURTLE_MASTER);
        addMix(Potions.TURTLE_MASTER, Items.REDSTONE, Potions.LONG_TURTLE_MASTER);
        addMix(Potions.TURTLE_MASTER, Items.GLOWSTONE_DUST, Potions.STRONG_TURTLE_MASTER);
        addMix(Potions.SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
        addMix(Potions.LONG_SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
        addMix(Potions.AWKWARD, Items.SUGAR, Potions.SWIFTNESS);
        addMix(Potions.SWIFTNESS, Items.REDSTONE, Potions.LONG_SWIFTNESS);
        addMix(Potions.SWIFTNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SWIFTNESS);
        addMix(Potions.AWKWARD, Items.PUFFERFISH, Potions.WATER_BREATHING);
        addMix(Potions.WATER_BREATHING, Items.REDSTONE, Potions.LONG_WATER_BREATHING);
        addMix(Potions.AWKWARD, Items.GLISTERING_MELON_SLICE, Potions.HEALING);
        addMix(Potions.HEALING, Items.GLOWSTONE_DUST, Potions.STRONG_HEALING);
        addMix(Potions.HEALING, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
        addMix(Potions.STRONG_HEALING, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
        addMix(Potions.HARMING, Items.GLOWSTONE_DUST, Potions.STRONG_HARMING);
        addMix(Potions.POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
        addMix(Potions.LONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
        addMix(Potions.STRONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
        addMix(Potions.AWKWARD, Items.SPIDER_EYE, Potions.POISON);
        addMix(Potions.POISON, Items.REDSTONE, Potions.LONG_POISON);
        addMix(Potions.POISON, Items.GLOWSTONE_DUST, Potions.STRONG_POISON);
        addMix(Potions.AWKWARD, Items.GHAST_TEAR, Potions.REGENERATION);
        addMix(Potions.REGENERATION, Items.REDSTONE, Potions.LONG_REGENERATION);
        addMix(Potions.REGENERATION, Items.GLOWSTONE_DUST, Potions.STRONG_REGENERATION);
        addMix(Potions.AWKWARD, Items.BLAZE_POWDER, Potions.STRENGTH);
        addMix(Potions.STRENGTH, Items.REDSTONE, Potions.LONG_STRENGTH);
        addMix(Potions.STRENGTH, Items.GLOWSTONE_DUST, Potions.STRONG_STRENGTH);
        addMix(Potions.WATER, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS);
        addMix(Potions.WEAKNESS, Items.REDSTONE, Potions.LONG_WEAKNESS);
        addMix(Potions.AWKWARD, Items.PHANTOM_MEMBRANE, Potions.SLOW_FALLING);
        addMix(Potions.SLOW_FALLING, Items.REDSTONE, Potions.LONG_SLOW_FALLING);
    }

    private static void addContainerRecipe(Item input, Item ingredient, Item output) {
        if (!(input instanceof ItemPotion)) {
            throw new IllegalArgumentException("Expected a potion, got: " + IRegistry.ITEM.getKey(input));
        } else if (!(output instanceof ItemPotion)) {
            throw new IllegalArgumentException("Expected a potion, got: " + IRegistry.ITEM.getKey(output));
        } else {
            CONTAINER_MIXES.add(new PotionBrewer.PredicatedCombination<>(input, RecipeItemStack.of(ingredient), output));
        }
    }

    private static void addContainer(Item item) {
        if (!(item instanceof ItemPotion)) {
            throw new IllegalArgumentException("Expected a potion, got: " + IRegistry.ITEM.getKey(item));
        } else {
            ALLOWED_CONTAINERS.add(RecipeItemStack.of(item));
        }
    }

    private static void addMix(PotionRegistry input, Item item, PotionRegistry output) {
        POTION_MIXES.add(new PotionBrewer.PredicatedCombination<>(input, RecipeItemStack.of(item), output));
    }

    static class PredicatedCombination<T> {
        final T from;
        final RecipeItemStack ingredient;
        final T to;

        public PredicatedCombination(T input, RecipeItemStack ingredient, T output) {
            this.from = input;
            this.ingredient = ingredient;
            this.to = output;
        }
    }
}
