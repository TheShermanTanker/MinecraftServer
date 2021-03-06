package net.minecraft.data.recipes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.critereon.CriterionConditionEntity;
import net.minecraft.advancements.critereon.CriterionConditionItem;
import net.minecraft.advancements.critereon.CriterionConditionValue;
import net.minecraft.advancements.critereon.CriterionTriggerEnterBlock;
import net.minecraft.advancements.critereon.CriterionTriggerImpossible;
import net.minecraft.advancements.critereon.CriterionTriggerInventoryChanged;
import net.minecraft.advancements.critereon.CriterionTriggerProperties;
import net.minecraft.core.IRegistry;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.data.DebugReportProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeSerializerCooking;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DebugReportProviderRecipe implements DebugReportProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private static final ImmutableList<IMaterial> COAL_SMELTABLES = ImmutableList.of(Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE);
    private static final ImmutableList<IMaterial> IRON_SMELTABLES = ImmutableList.of(Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.RAW_IRON);
    private static final ImmutableList<IMaterial> COPPER_SMELTABLES = ImmutableList.of(Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER);
    private static final ImmutableList<IMaterial> GOLD_SMELTABLES = ImmutableList.of(Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, Items.NETHER_GOLD_ORE, Items.RAW_GOLD);
    private static final ImmutableList<IMaterial> DIAMOND_SMELTABLES = ImmutableList.of(Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE);
    private static final ImmutableList<IMaterial> LAPIS_SMELTABLES = ImmutableList.of(Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE);
    private static final ImmutableList<IMaterial> REDSTONE_SMELTABLES = ImmutableList.of(Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE);
    private static final ImmutableList<IMaterial> EMERALD_SMELTABLES = ImmutableList.of(Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE);
    private final DebugReportGenerator generator;
    private static final Map<BlockFamily.Variant, BiFunction<IMaterial, IMaterial, IRecipeBuilder>> shapeBuilders = ImmutableMap.<BlockFamily.Variant, BiFunction<IMaterial, IMaterial, IRecipeBuilder>>builder().put(BlockFamily.Variant.BUTTON, (output, input) -> {
        return buttonBuilder(output, RecipeItemStack.of(input));
    }).put(BlockFamily.Variant.CHISELED, (output, input) -> {
        return chiseledBuilder(output, RecipeItemStack.of(input));
    }).put(BlockFamily.Variant.CUT, (output, input) -> {
        return cutBuilder(output, RecipeItemStack.of(input));
    }).put(BlockFamily.Variant.DOOR, (output, input) -> {
        return doorBuilder(output, RecipeItemStack.of(input));
    }).put(BlockFamily.Variant.FENCE, (output, input) -> {
        return fenceBuilder(output, RecipeItemStack.of(input));
    }).put(BlockFamily.Variant.FENCE_GATE, (output, input) -> {
        return fenceGateBuilder(output, RecipeItemStack.of(input));
    }).put(BlockFamily.Variant.SIGN, (output, input) -> {
        return signBuilder(output, RecipeItemStack.of(input));
    }).put(BlockFamily.Variant.SLAB, (output, input) -> {
        return slabBuilder(output, RecipeItemStack.of(input));
    }).put(BlockFamily.Variant.STAIRS, (output, input) -> {
        return stairBuilder(output, RecipeItemStack.of(input));
    }).put(BlockFamily.Variant.PRESSURE_PLATE, (output, input) -> {
        return pressurePlateBuilder(output, RecipeItemStack.of(input));
    }).put(BlockFamily.Variant.POLISHED, (output, input) -> {
        return polishedBuilder(output, RecipeItemStack.of(input));
    }).put(BlockFamily.Variant.TRAPDOOR, (output, input) -> {
        return trapdoorBuilder(output, RecipeItemStack.of(input));
    }).put(BlockFamily.Variant.WALL, (output, input) -> {
        return wallBuilder(output, RecipeItemStack.of(input));
    }).build();

    public DebugReportProviderRecipe(DebugReportGenerator root) {
        this.generator = root;
    }

    @Override
    public void run(HashCache cache) {
        Path path = this.generator.getOutputFolder();
        Set<MinecraftKey> set = Sets.newHashSet();
        buildCraftingRecipes((provider) -> {
            if (!set.add(provider.getId())) {
                throw new IllegalStateException("Duplicate recipe " + provider.getId());
            } else {
                saveRecipe(cache, provider.serializeRecipe(), path.resolve("data/" + provider.getId().getNamespace() + "/recipes/" + provider.getId().getKey() + ".json"));
                JsonObject jsonObject = provider.serializeAdvancement();
                if (jsonObject != null) {
                    saveAdvancement(cache, jsonObject, path.resolve("data/" + provider.getId().getNamespace() + "/advancements/" + provider.getAdvancementId().getKey() + ".json"));
                }

            }
        });
        saveAdvancement(cache, Advancement.SerializedAdvancement.advancement().addCriterion("impossible", new CriterionTriggerImpossible.CriterionInstanceTrigger()).serializeToJson(), path.resolve("data/minecraft/advancements/recipes/root.json"));
    }

    private static void saveRecipe(HashCache cache, JsonObject json, Path path) {
        try {
            String string = GSON.toJson((JsonElement)json);
            String string2 = SHA1.hashUnencodedChars(string).toString();
            if (!Objects.equals(cache.getHash(path), string2) || !Files.exists(path)) {
                Files.createDirectories(path.getParent());
                BufferedWriter bufferedWriter = Files.newBufferedWriter(path);

                try {
                    bufferedWriter.write(string);
                } catch (Throwable var9) {
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (Throwable var8) {
                            var9.addSuppressed(var8);
                        }
                    }

                    throw var9;
                }

                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            }

            cache.putNew(path, string2);
        } catch (IOException var10) {
            LOGGER.error("Couldn't save recipe {}", path, var10);
        }

    }

    private static void saveAdvancement(HashCache cache, JsonObject json, Path path) {
        try {
            String string = GSON.toJson((JsonElement)json);
            String string2 = SHA1.hashUnencodedChars(string).toString();
            if (!Objects.equals(cache.getHash(path), string2) || !Files.exists(path)) {
                Files.createDirectories(path.getParent());
                BufferedWriter bufferedWriter = Files.newBufferedWriter(path);

                try {
                    bufferedWriter.write(string);
                } catch (Throwable var9) {
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (Throwable var8) {
                            var9.addSuppressed(var8);
                        }
                    }

                    throw var9;
                }

                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            }

            cache.putNew(path, string2);
        } catch (IOException var10) {
            LOGGER.error("Couldn't save recipe advancement {}", path, var10);
        }

    }

    private static void buildCraftingRecipes(Consumer<IFinishedRecipe> exporter) {
        BlockFamilies.getAllFamilies().filter(BlockFamily::shouldGenerateRecipe).forEach((family) -> {
            generateRecipes(exporter, family);
        });
        planksFromLog(exporter, Blocks.ACACIA_PLANKS, TagsItem.ACACIA_LOGS);
        planksFromLogs(exporter, Blocks.BIRCH_PLANKS, TagsItem.BIRCH_LOGS);
        planksFromLogs(exporter, Blocks.CRIMSON_PLANKS, TagsItem.CRIMSON_STEMS);
        planksFromLog(exporter, Blocks.DARK_OAK_PLANKS, TagsItem.DARK_OAK_LOGS);
        planksFromLogs(exporter, Blocks.JUNGLE_PLANKS, TagsItem.JUNGLE_LOGS);
        planksFromLogs(exporter, Blocks.OAK_PLANKS, TagsItem.OAK_LOGS);
        planksFromLogs(exporter, Blocks.SPRUCE_PLANKS, TagsItem.SPRUCE_LOGS);
        planksFromLogs(exporter, Blocks.WARPED_PLANKS, TagsItem.WARPED_STEMS);
        woodFromLogs(exporter, Blocks.ACACIA_WOOD, Blocks.ACACIA_LOG);
        woodFromLogs(exporter, Blocks.BIRCH_WOOD, Blocks.BIRCH_LOG);
        woodFromLogs(exporter, Blocks.DARK_OAK_WOOD, Blocks.DARK_OAK_LOG);
        woodFromLogs(exporter, Blocks.JUNGLE_WOOD, Blocks.JUNGLE_LOG);
        woodFromLogs(exporter, Blocks.OAK_WOOD, Blocks.OAK_LOG);
        woodFromLogs(exporter, Blocks.SPRUCE_WOOD, Blocks.SPRUCE_LOG);
        woodFromLogs(exporter, Blocks.CRIMSON_HYPHAE, Blocks.CRIMSON_STEM);
        woodFromLogs(exporter, Blocks.WARPED_HYPHAE, Blocks.WARPED_STEM);
        woodFromLogs(exporter, Blocks.STRIPPED_ACACIA_WOOD, Blocks.STRIPPED_ACACIA_LOG);
        woodFromLogs(exporter, Blocks.STRIPPED_BIRCH_WOOD, Blocks.STRIPPED_BIRCH_LOG);
        woodFromLogs(exporter, Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_LOG);
        woodFromLogs(exporter, Blocks.STRIPPED_JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_LOG);
        woodFromLogs(exporter, Blocks.STRIPPED_OAK_WOOD, Blocks.STRIPPED_OAK_LOG);
        woodFromLogs(exporter, Blocks.STRIPPED_SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_LOG);
        woodFromLogs(exporter, Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_STEM);
        woodFromLogs(exporter, Blocks.STRIPPED_WARPED_HYPHAE, Blocks.STRIPPED_WARPED_STEM);
        woodenBoat(exporter, Items.ACACIA_BOAT, Blocks.ACACIA_PLANKS);
        woodenBoat(exporter, Items.BIRCH_BOAT, Blocks.BIRCH_PLANKS);
        woodenBoat(exporter, Items.DARK_OAK_BOAT, Blocks.DARK_OAK_PLANKS);
        woodenBoat(exporter, Items.JUNGLE_BOAT, Blocks.JUNGLE_PLANKS);
        woodenBoat(exporter, Items.OAK_BOAT, Blocks.OAK_PLANKS);
        woodenBoat(exporter, Items.SPRUCE_BOAT, Blocks.SPRUCE_PLANKS);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.BLACK_WOOL, Items.BLACK_DYE);
        carpet(exporter, Blocks.BLACK_CARPET, Blocks.BLACK_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.BLACK_CARPET, Items.BLACK_DYE);
        bedFromPlanksAndWool(exporter, Items.BLACK_BED, Blocks.BLACK_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.BLACK_BED, Items.BLACK_DYE);
        banner(exporter, Items.BLACK_BANNER, Blocks.BLACK_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.BLUE_WOOL, Items.BLUE_DYE);
        carpet(exporter, Blocks.BLUE_CARPET, Blocks.BLUE_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.BLUE_CARPET, Items.BLUE_DYE);
        bedFromPlanksAndWool(exporter, Items.BLUE_BED, Blocks.BLUE_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.BLUE_BED, Items.BLUE_DYE);
        banner(exporter, Items.BLUE_BANNER, Blocks.BLUE_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.BROWN_WOOL, Items.BROWN_DYE);
        carpet(exporter, Blocks.BROWN_CARPET, Blocks.BROWN_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.BROWN_CARPET, Items.BROWN_DYE);
        bedFromPlanksAndWool(exporter, Items.BROWN_BED, Blocks.BROWN_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.BROWN_BED, Items.BROWN_DYE);
        banner(exporter, Items.BROWN_BANNER, Blocks.BROWN_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.CYAN_WOOL, Items.CYAN_DYE);
        carpet(exporter, Blocks.CYAN_CARPET, Blocks.CYAN_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.CYAN_CARPET, Items.CYAN_DYE);
        bedFromPlanksAndWool(exporter, Items.CYAN_BED, Blocks.CYAN_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.CYAN_BED, Items.CYAN_DYE);
        banner(exporter, Items.CYAN_BANNER, Blocks.CYAN_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.GRAY_WOOL, Items.GRAY_DYE);
        carpet(exporter, Blocks.GRAY_CARPET, Blocks.GRAY_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.GRAY_CARPET, Items.GRAY_DYE);
        bedFromPlanksAndWool(exporter, Items.GRAY_BED, Blocks.GRAY_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.GRAY_BED, Items.GRAY_DYE);
        banner(exporter, Items.GRAY_BANNER, Blocks.GRAY_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.GREEN_WOOL, Items.GREEN_DYE);
        carpet(exporter, Blocks.GREEN_CARPET, Blocks.GREEN_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.GREEN_CARPET, Items.GREEN_DYE);
        bedFromPlanksAndWool(exporter, Items.GREEN_BED, Blocks.GREEN_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.GREEN_BED, Items.GREEN_DYE);
        banner(exporter, Items.GREEN_BANNER, Blocks.GREEN_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.LIGHT_BLUE_WOOL, Items.LIGHT_BLUE_DYE);
        carpet(exporter, Blocks.LIGHT_BLUE_CARPET, Blocks.LIGHT_BLUE_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.LIGHT_BLUE_CARPET, Items.LIGHT_BLUE_DYE);
        bedFromPlanksAndWool(exporter, Items.LIGHT_BLUE_BED, Blocks.LIGHT_BLUE_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.LIGHT_BLUE_BED, Items.LIGHT_BLUE_DYE);
        banner(exporter, Items.LIGHT_BLUE_BANNER, Blocks.LIGHT_BLUE_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.LIGHT_GRAY_WOOL, Items.LIGHT_GRAY_DYE);
        carpet(exporter, Blocks.LIGHT_GRAY_CARPET, Blocks.LIGHT_GRAY_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.LIGHT_GRAY_CARPET, Items.LIGHT_GRAY_DYE);
        bedFromPlanksAndWool(exporter, Items.LIGHT_GRAY_BED, Blocks.LIGHT_GRAY_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.LIGHT_GRAY_BED, Items.LIGHT_GRAY_DYE);
        banner(exporter, Items.LIGHT_GRAY_BANNER, Blocks.LIGHT_GRAY_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.LIME_WOOL, Items.LIME_DYE);
        carpet(exporter, Blocks.LIME_CARPET, Blocks.LIME_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.LIME_CARPET, Items.LIME_DYE);
        bedFromPlanksAndWool(exporter, Items.LIME_BED, Blocks.LIME_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.LIME_BED, Items.LIME_DYE);
        banner(exporter, Items.LIME_BANNER, Blocks.LIME_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.MAGENTA_WOOL, Items.MAGENTA_DYE);
        carpet(exporter, Blocks.MAGENTA_CARPET, Blocks.MAGENTA_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.MAGENTA_CARPET, Items.MAGENTA_DYE);
        bedFromPlanksAndWool(exporter, Items.MAGENTA_BED, Blocks.MAGENTA_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.MAGENTA_BED, Items.MAGENTA_DYE);
        banner(exporter, Items.MAGENTA_BANNER, Blocks.MAGENTA_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.ORANGE_WOOL, Items.ORANGE_DYE);
        carpet(exporter, Blocks.ORANGE_CARPET, Blocks.ORANGE_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.ORANGE_CARPET, Items.ORANGE_DYE);
        bedFromPlanksAndWool(exporter, Items.ORANGE_BED, Blocks.ORANGE_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.ORANGE_BED, Items.ORANGE_DYE);
        banner(exporter, Items.ORANGE_BANNER, Blocks.ORANGE_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.PINK_WOOL, Items.PINK_DYE);
        carpet(exporter, Blocks.PINK_CARPET, Blocks.PINK_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.PINK_CARPET, Items.PINK_DYE);
        bedFromPlanksAndWool(exporter, Items.PINK_BED, Blocks.PINK_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.PINK_BED, Items.PINK_DYE);
        banner(exporter, Items.PINK_BANNER, Blocks.PINK_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.PURPLE_WOOL, Items.PURPLE_DYE);
        carpet(exporter, Blocks.PURPLE_CARPET, Blocks.PURPLE_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.PURPLE_CARPET, Items.PURPLE_DYE);
        bedFromPlanksAndWool(exporter, Items.PURPLE_BED, Blocks.PURPLE_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.PURPLE_BED, Items.PURPLE_DYE);
        banner(exporter, Items.PURPLE_BANNER, Blocks.PURPLE_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.RED_WOOL, Items.RED_DYE);
        carpet(exporter, Blocks.RED_CARPET, Blocks.RED_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.RED_CARPET, Items.RED_DYE);
        bedFromPlanksAndWool(exporter, Items.RED_BED, Blocks.RED_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.RED_BED, Items.RED_DYE);
        banner(exporter, Items.RED_BANNER, Blocks.RED_WOOL);
        carpet(exporter, Blocks.WHITE_CARPET, Blocks.WHITE_WOOL);
        bedFromPlanksAndWool(exporter, Items.WHITE_BED, Blocks.WHITE_WOOL);
        banner(exporter, Items.WHITE_BANNER, Blocks.WHITE_WOOL);
        coloredWoolFromWhiteWoolAndDye(exporter, Blocks.YELLOW_WOOL, Items.YELLOW_DYE);
        carpet(exporter, Blocks.YELLOW_CARPET, Blocks.YELLOW_WOOL);
        coloredCarpetFromWhiteCarpetAndDye(exporter, Blocks.YELLOW_CARPET, Items.YELLOW_DYE);
        bedFromPlanksAndWool(exporter, Items.YELLOW_BED, Blocks.YELLOW_WOOL);
        bedFromWhiteBedAndDye(exporter, Items.YELLOW_BED, Items.YELLOW_DYE);
        banner(exporter, Items.YELLOW_BANNER, Blocks.YELLOW_WOOL);
        carpet(exporter, Blocks.MOSS_CARPET, Blocks.MOSS_BLOCK);
        stainedGlassFromGlassAndDye(exporter, Blocks.BLACK_STAINED_GLASS, Items.BLACK_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.BLACK_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.BLACK_STAINED_GLASS_PANE, Items.BLACK_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.BLUE_STAINED_GLASS, Items.BLUE_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.BLUE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.BLUE_STAINED_GLASS_PANE, Items.BLUE_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.BROWN_STAINED_GLASS, Items.BROWN_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.BROWN_STAINED_GLASS_PANE, Blocks.BROWN_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.BROWN_STAINED_GLASS_PANE, Items.BROWN_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.CYAN_STAINED_GLASS, Items.CYAN_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.CYAN_STAINED_GLASS_PANE, Blocks.CYAN_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.CYAN_STAINED_GLASS_PANE, Items.CYAN_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.GRAY_STAINED_GLASS, Items.GRAY_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.GRAY_STAINED_GLASS_PANE, Blocks.GRAY_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.GRAY_STAINED_GLASS_PANE, Items.GRAY_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.GREEN_STAINED_GLASS, Items.GREEN_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.GREEN_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.GREEN_STAINED_GLASS_PANE, Items.GREEN_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.LIGHT_BLUE_STAINED_GLASS, Items.LIGHT_BLUE_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.LIGHT_BLUE_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Items.LIGHT_BLUE_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.LIGHT_GRAY_STAINED_GLASS, Items.LIGHT_GRAY_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Items.LIGHT_GRAY_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.LIME_STAINED_GLASS, Items.LIME_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.LIME_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.LIME_STAINED_GLASS_PANE, Items.LIME_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.MAGENTA_STAINED_GLASS, Items.MAGENTA_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.MAGENTA_STAINED_GLASS_PANE, Blocks.MAGENTA_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.MAGENTA_STAINED_GLASS_PANE, Items.MAGENTA_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.ORANGE_STAINED_GLASS, Items.ORANGE_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.ORANGE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.ORANGE_STAINED_GLASS_PANE, Items.ORANGE_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.PINK_STAINED_GLASS, Items.PINK_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.PINK_STAINED_GLASS_PANE, Blocks.PINK_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.PINK_STAINED_GLASS_PANE, Items.PINK_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.PURPLE_STAINED_GLASS, Items.PURPLE_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.PURPLE_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.PURPLE_STAINED_GLASS_PANE, Items.PURPLE_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.RED_STAINED_GLASS, Items.RED_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.RED_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.RED_STAINED_GLASS_PANE, Items.RED_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.WHITE_STAINED_GLASS, Items.WHITE_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.WHITE_STAINED_GLASS_PANE, Blocks.WHITE_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.WHITE_STAINED_GLASS_PANE, Items.WHITE_DYE);
        stainedGlassFromGlassAndDye(exporter, Blocks.YELLOW_STAINED_GLASS, Items.YELLOW_DYE);
        stainedGlassPaneFromStainedGlass(exporter, Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS);
        stainedGlassPaneFromGlassPaneAndDye(exporter, Blocks.YELLOW_STAINED_GLASS_PANE, Items.YELLOW_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.BLACK_TERRACOTTA, Items.BLACK_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.BLUE_TERRACOTTA, Items.BLUE_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.BROWN_TERRACOTTA, Items.BROWN_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.CYAN_TERRACOTTA, Items.CYAN_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.GRAY_TERRACOTTA, Items.GRAY_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.GREEN_TERRACOTTA, Items.GREEN_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.LIGHT_BLUE_TERRACOTTA, Items.LIGHT_BLUE_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.LIGHT_GRAY_TERRACOTTA, Items.LIGHT_GRAY_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.LIME_TERRACOTTA, Items.LIME_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.MAGENTA_TERRACOTTA, Items.MAGENTA_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.ORANGE_TERRACOTTA, Items.ORANGE_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.PINK_TERRACOTTA, Items.PINK_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.PURPLE_TERRACOTTA, Items.PURPLE_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.RED_TERRACOTTA, Items.RED_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.WHITE_TERRACOTTA, Items.WHITE_DYE);
        coloredTerracottaFromTerracottaAndDye(exporter, Blocks.YELLOW_TERRACOTTA, Items.YELLOW_DYE);
        concretePowder(exporter, Blocks.BLACK_CONCRETE_POWDER, Items.BLACK_DYE);
        concretePowder(exporter, Blocks.BLUE_CONCRETE_POWDER, Items.BLUE_DYE);
        concretePowder(exporter, Blocks.BROWN_CONCRETE_POWDER, Items.BROWN_DYE);
        concretePowder(exporter, Blocks.CYAN_CONCRETE_POWDER, Items.CYAN_DYE);
        concretePowder(exporter, Blocks.GRAY_CONCRETE_POWDER, Items.GRAY_DYE);
        concretePowder(exporter, Blocks.GREEN_CONCRETE_POWDER, Items.GREEN_DYE);
        concretePowder(exporter, Blocks.LIGHT_BLUE_CONCRETE_POWDER, Items.LIGHT_BLUE_DYE);
        concretePowder(exporter, Blocks.LIGHT_GRAY_CONCRETE_POWDER, Items.LIGHT_GRAY_DYE);
        concretePowder(exporter, Blocks.LIME_CONCRETE_POWDER, Items.LIME_DYE);
        concretePowder(exporter, Blocks.MAGENTA_CONCRETE_POWDER, Items.MAGENTA_DYE);
        concretePowder(exporter, Blocks.ORANGE_CONCRETE_POWDER, Items.ORANGE_DYE);
        concretePowder(exporter, Blocks.PINK_CONCRETE_POWDER, Items.PINK_DYE);
        concretePowder(exporter, Blocks.PURPLE_CONCRETE_POWDER, Items.PURPLE_DYE);
        concretePowder(exporter, Blocks.RED_CONCRETE_POWDER, Items.RED_DYE);
        concretePowder(exporter, Blocks.WHITE_CONCRETE_POWDER, Items.WHITE_DYE);
        concretePowder(exporter, Blocks.YELLOW_CONCRETE_POWDER, Items.YELLOW_DYE);
        RecipeBuilderShaped.shaped(Items.CANDLE).define('S', Items.STRING).define('H', Items.HONEYCOMB).pattern("S").pattern("H").unlockedBy("has_string", has(Items.STRING)).unlockedBy("has_honeycomb", has(Items.HONEYCOMB)).save(exporter);
        candle(exporter, Blocks.BLACK_CANDLE, Items.BLACK_DYE);
        candle(exporter, Blocks.BLUE_CANDLE, Items.BLUE_DYE);
        candle(exporter, Blocks.BROWN_CANDLE, Items.BROWN_DYE);
        candle(exporter, Blocks.CYAN_CANDLE, Items.CYAN_DYE);
        candle(exporter, Blocks.GRAY_CANDLE, Items.GRAY_DYE);
        candle(exporter, Blocks.GREEN_CANDLE, Items.GREEN_DYE);
        candle(exporter, Blocks.LIGHT_BLUE_CANDLE, Items.LIGHT_BLUE_DYE);
        candle(exporter, Blocks.LIGHT_GRAY_CANDLE, Items.LIGHT_GRAY_DYE);
        candle(exporter, Blocks.LIME_CANDLE, Items.LIME_DYE);
        candle(exporter, Blocks.MAGENTA_CANDLE, Items.MAGENTA_DYE);
        candle(exporter, Blocks.ORANGE_CANDLE, Items.ORANGE_DYE);
        candle(exporter, Blocks.PINK_CANDLE, Items.PINK_DYE);
        candle(exporter, Blocks.PURPLE_CANDLE, Items.PURPLE_DYE);
        candle(exporter, Blocks.RED_CANDLE, Items.RED_DYE);
        candle(exporter, Blocks.WHITE_CANDLE, Items.WHITE_DYE);
        candle(exporter, Blocks.YELLOW_CANDLE, Items.YELLOW_DYE);
        RecipeBuilderShaped.shaped(Blocks.ACTIVATOR_RAIL, 6).define('#', Blocks.REDSTONE_TORCH).define('S', Items.STICK).define('X', Items.IRON_INGOT).pattern("XSX").pattern("X#X").pattern("XSX").unlockedBy("has_rail", has(Blocks.RAIL)).save(exporter);
        RecipeBuilderShapeless.shapeless(Blocks.ANDESITE, 2).requires(Blocks.DIORITE).requires(Blocks.COBBLESTONE).unlockedBy("has_stone", has(Blocks.DIORITE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.ANVIL).define('I', Blocks.IRON_BLOCK).define('i', Items.IRON_INGOT).pattern("III").pattern(" i ").pattern("iii").unlockedBy("has_iron_block", has(Blocks.IRON_BLOCK)).save(exporter);
        RecipeBuilderShaped.shaped(Items.ARMOR_STAND).define('/', Items.STICK).define('_', Blocks.SMOOTH_STONE_SLAB).pattern("///").pattern(" / ").pattern("/_/").unlockedBy("has_stone_slab", has(Blocks.SMOOTH_STONE_SLAB)).save(exporter);
        RecipeBuilderShaped.shaped(Items.ARROW, 4).define('#', Items.STICK).define('X', Items.FLINT).define('Y', Items.FEATHER).pattern("X").pattern("#").pattern("Y").unlockedBy("has_feather", has(Items.FEATHER)).unlockedBy("has_flint", has(Items.FLINT)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.BARREL, 1).define('P', TagsItem.PLANKS).define('S', TagsItem.WOODEN_SLABS).pattern("PSP").pattern("P P").pattern("PSP").unlockedBy("has_planks", has(TagsItem.PLANKS)).unlockedBy("has_wood_slab", has(TagsItem.WOODEN_SLABS)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.BEACON).define('S', Items.NETHER_STAR).define('G', Blocks.GLASS).define('O', Blocks.OBSIDIAN).pattern("GGG").pattern("GSG").pattern("OOO").unlockedBy("has_nether_star", has(Items.NETHER_STAR)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.BEEHIVE).define('P', TagsItem.PLANKS).define('H', Items.HONEYCOMB).pattern("PPP").pattern("HHH").pattern("PPP").unlockedBy("has_honeycomb", has(Items.HONEYCOMB)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.BEETROOT_SOUP).requires(Items.BOWL).requires(Items.BEETROOT, 6).unlockedBy("has_beetroot", has(Items.BEETROOT)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.BLACK_DYE).requires(Items.INK_SAC).group("black_dye").unlockedBy("has_ink_sac", has(Items.INK_SAC)).save(exporter);
        oneToOneConversionRecipe(exporter, Items.BLACK_DYE, Blocks.WITHER_ROSE, "black_dye");
        RecipeBuilderShapeless.shapeless(Items.BLAZE_POWDER, 2).requires(Items.BLAZE_ROD).unlockedBy("has_blaze_rod", has(Items.BLAZE_ROD)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.BLUE_DYE).requires(Items.LAPIS_LAZULI).group("blue_dye").unlockedBy("has_lapis_lazuli", has(Items.LAPIS_LAZULI)).save(exporter);
        oneToOneConversionRecipe(exporter, Items.BLUE_DYE, Blocks.CORNFLOWER, "blue_dye");
        RecipeBuilderShaped.shaped(Blocks.BLUE_ICE).define('#', Blocks.PACKED_ICE).pattern("###").pattern("###").pattern("###").unlockedBy("has_packed_ice", has(Blocks.PACKED_ICE)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.BONE_MEAL, 3).requires(Items.BONE).group("bonemeal").unlockedBy("has_bone", has(Items.BONE)).save(exporter);
        nineBlockStorageRecipesRecipesWithCustomUnpacking(exporter, Items.BONE_MEAL, Items.BONE_BLOCK, "bone_meal_from_bone_block", "bonemeal");
        RecipeBuilderShapeless.shapeless(Items.BOOK).requires(Items.PAPER, 3).requires(Items.LEATHER).unlockedBy("has_paper", has(Items.PAPER)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.BOOKSHELF).define('#', TagsItem.PLANKS).define('X', Items.BOOK).pattern("###").pattern("XXX").pattern("###").unlockedBy("has_book", has(Items.BOOK)).save(exporter);
        RecipeBuilderShaped.shaped(Items.BOW).define('#', Items.STICK).define('X', Items.STRING).pattern(" #X").pattern("# X").pattern(" #X").unlockedBy("has_string", has(Items.STRING)).save(exporter);
        RecipeBuilderShaped.shaped(Items.BOWL, 4).define('#', TagsItem.PLANKS).pattern("# #").pattern(" # ").unlockedBy("has_brown_mushroom", has(Blocks.BROWN_MUSHROOM)).unlockedBy("has_red_mushroom", has(Blocks.RED_MUSHROOM)).unlockedBy("has_mushroom_stew", has(Items.MUSHROOM_STEW)).save(exporter);
        RecipeBuilderShaped.shaped(Items.BREAD).define('#', Items.WHEAT).pattern("###").unlockedBy("has_wheat", has(Items.WHEAT)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.BREWING_STAND).define('B', Items.BLAZE_ROD).define('#', TagsItem.STONE_CRAFTING_MATERIALS).pattern(" B ").pattern("###").unlockedBy("has_blaze_rod", has(Items.BLAZE_ROD)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.BRICKS).define('#', Items.BRICK).pattern("##").pattern("##").unlockedBy("has_brick", has(Items.BRICK)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.BROWN_DYE).requires(Items.COCOA_BEANS).group("brown_dye").unlockedBy("has_cocoa_beans", has(Items.COCOA_BEANS)).save(exporter);
        RecipeBuilderShaped.shaped(Items.BUCKET).define('#', Items.IRON_INGOT).pattern("# #").pattern(" # ").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.CAKE).define('A', Items.MILK_BUCKET).define('B', Items.SUGAR).define('C', Items.WHEAT).define('E', Items.EGG).pattern("AAA").pattern("BEB").pattern("CCC").unlockedBy("has_egg", has(Items.EGG)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.CAMPFIRE).define('L', TagsItem.LOGS).define('S', Items.STICK).define('C', TagsItem.COALS).pattern(" S ").pattern("SCS").pattern("LLL").unlockedBy("has_stick", has(Items.STICK)).unlockedBy("has_coal", has(TagsItem.COALS)).save(exporter);
        RecipeBuilderShaped.shaped(Items.CARROT_ON_A_STICK).define('#', Items.FISHING_ROD).define('X', Items.CARROT).pattern("# ").pattern(" X").unlockedBy("has_carrot", has(Items.CARROT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.WARPED_FUNGUS_ON_A_STICK).define('#', Items.FISHING_ROD).define('X', Items.WARPED_FUNGUS).pattern("# ").pattern(" X").unlockedBy("has_warped_fungus", has(Items.WARPED_FUNGUS)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.CAULDRON).define('#', Items.IRON_INGOT).pattern("# #").pattern("# #").pattern("###").unlockedBy("has_water_bucket", has(Items.WATER_BUCKET)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.COMPOSTER).define('#', TagsItem.WOODEN_SLABS).pattern("# #").pattern("# #").pattern("###").unlockedBy("has_wood_slab", has(TagsItem.WOODEN_SLABS)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.CHEST).define('#', TagsItem.PLANKS).pattern("###").pattern("# #").pattern("###").unlockedBy("has_lots_of_items", new CriterionTriggerInventoryChanged.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, CriterionConditionValue.IntegerRange.atLeast(10), CriterionConditionValue.IntegerRange.ANY, CriterionConditionValue.IntegerRange.ANY, new CriterionConditionItem[0])).save(exporter);
        RecipeBuilderShaped.shaped(Items.CHEST_MINECART).define('A', Blocks.CHEST).define('B', Items.MINECART).pattern("A").pattern("B").unlockedBy("has_minecart", has(Items.MINECART)).save(exporter);
        chiseledBuilder(Blocks.CHISELED_QUARTZ_BLOCK, RecipeItemStack.of(Blocks.QUARTZ_SLAB)).unlockedBy("has_chiseled_quartz_block", has(Blocks.CHISELED_QUARTZ_BLOCK)).unlockedBy("has_quartz_block", has(Blocks.QUARTZ_BLOCK)).unlockedBy("has_quartz_pillar", has(Blocks.QUARTZ_PILLAR)).save(exporter);
        chiseledBuilder(Blocks.CHISELED_STONE_BRICKS, RecipeItemStack.of(Blocks.STONE_BRICK_SLAB)).unlockedBy("has_tag", has(TagsItem.STONE_BRICKS)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.CLAY).define('#', Items.CLAY_BALL).pattern("##").pattern("##").unlockedBy("has_clay_ball", has(Items.CLAY_BALL)).save(exporter);
        RecipeBuilderShaped.shaped(Items.CLOCK).define('#', Items.GOLD_INGOT).define('X', Items.REDSTONE).pattern(" # ").pattern("#X#").pattern(" # ").unlockedBy("has_redstone", has(Items.REDSTONE)).save(exporter);
        nineBlockStorageRecipes(exporter, Items.COAL, Items.COAL_BLOCK);
        RecipeBuilderShaped.shaped(Blocks.COARSE_DIRT, 4).define('D', Blocks.DIRT).define('G', Blocks.GRAVEL).pattern("DG").pattern("GD").unlockedBy("has_gravel", has(Blocks.GRAVEL)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.COMPARATOR).define('#', Blocks.REDSTONE_TORCH).define('X', Items.QUARTZ).define('I', Blocks.STONE).pattern(" # ").pattern("#X#").pattern("III").unlockedBy("has_quartz", has(Items.QUARTZ)).save(exporter);
        RecipeBuilderShaped.shaped(Items.COMPASS).define('#', Items.IRON_INGOT).define('X', Items.REDSTONE).pattern(" # ").pattern("#X#").pattern(" # ").unlockedBy("has_redstone", has(Items.REDSTONE)).save(exporter);
        RecipeBuilderShaped.shaped(Items.COOKIE, 8).define('#', Items.WHEAT).define('X', Items.COCOA_BEANS).pattern("#X#").unlockedBy("has_cocoa", has(Items.COCOA_BEANS)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.CRAFTING_TABLE).define('#', TagsItem.PLANKS).pattern("##").pattern("##").unlockedBy("has_planks", has(TagsItem.PLANKS)).save(exporter);
        RecipeBuilderShaped.shaped(Items.CROSSBOW).define('~', Items.STRING).define('#', Items.STICK).define('&', Items.IRON_INGOT).define('$', Blocks.TRIPWIRE_HOOK).pattern("#&#").pattern("~$~").pattern(" # ").unlockedBy("has_string", has(Items.STRING)).unlockedBy("has_stick", has(Items.STICK)).unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).unlockedBy("has_tripwire_hook", has(Blocks.TRIPWIRE_HOOK)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.LOOM).define('#', TagsItem.PLANKS).define('@', Items.STRING).pattern("@@").pattern("##").unlockedBy("has_string", has(Items.STRING)).save(exporter);
        chiseledBuilder(Blocks.CHISELED_RED_SANDSTONE, RecipeItemStack.of(Blocks.RED_SANDSTONE_SLAB)).unlockedBy("has_red_sandstone", has(Blocks.RED_SANDSTONE)).unlockedBy("has_chiseled_red_sandstone", has(Blocks.CHISELED_RED_SANDSTONE)).unlockedBy("has_cut_red_sandstone", has(Blocks.CUT_RED_SANDSTONE)).save(exporter);
        chiseled(exporter, Blocks.CHISELED_SANDSTONE, Blocks.SANDSTONE_SLAB);
        nineBlockStorageRecipesRecipesWithCustomUnpacking(exporter, Items.COPPER_INGOT, Items.COPPER_BLOCK, getSimpleRecipeName(Items.COPPER_INGOT), getItemName(Items.COPPER_INGOT));
        RecipeBuilderShapeless.shapeless(Items.COPPER_INGOT, 9).requires(Blocks.WAXED_COPPER_BLOCK).group(getItemName(Items.COPPER_INGOT)).unlockedBy(getHasName(Blocks.WAXED_COPPER_BLOCK), has(Blocks.WAXED_COPPER_BLOCK)).save(exporter, getConversionRecipeName(Items.COPPER_INGOT, Blocks.WAXED_COPPER_BLOCK));
        waxRecipes(exporter);
        RecipeBuilderShapeless.shapeless(Items.CYAN_DYE, 2).requires(Items.BLUE_DYE).requires(Items.GREEN_DYE).unlockedBy("has_green_dye", has(Items.GREEN_DYE)).unlockedBy("has_blue_dye", has(Items.BLUE_DYE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.DARK_PRISMARINE).define('S', Items.PRISMARINE_SHARD).define('I', Items.BLACK_DYE).pattern("SSS").pattern("SIS").pattern("SSS").unlockedBy("has_prismarine_shard", has(Items.PRISMARINE_SHARD)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.DAYLIGHT_DETECTOR).define('Q', Items.QUARTZ).define('G', Blocks.GLASS).define('W', RecipeItemStack.of(TagsItem.WOODEN_SLABS)).pattern("GGG").pattern("QQQ").pattern("WWW").unlockedBy("has_quartz", has(Items.QUARTZ)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.DEEPSLATE_BRICKS, 4).define('S', Blocks.POLISHED_DEEPSLATE).pattern("SS").pattern("SS").unlockedBy("has_polished_deepslate", has(Blocks.POLISHED_DEEPSLATE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.DEEPSLATE_TILES, 4).define('S', Blocks.DEEPSLATE_BRICKS).pattern("SS").pattern("SS").unlockedBy("has_deepslate_bricks", has(Blocks.DEEPSLATE_BRICKS)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.DETECTOR_RAIL, 6).define('R', Items.REDSTONE).define('#', Blocks.STONE_PRESSURE_PLATE).define('X', Items.IRON_INGOT).pattern("X X").pattern("X#X").pattern("XRX").unlockedBy("has_rail", has(Blocks.RAIL)).save(exporter);
        RecipeBuilderShaped.shaped(Items.DIAMOND_AXE).define('#', Items.STICK).define('X', Items.DIAMOND).pattern("XX").pattern("X#").pattern(" #").unlockedBy("has_diamond", has(Items.DIAMOND)).save(exporter);
        nineBlockStorageRecipes(exporter, Items.DIAMOND, Items.DIAMOND_BLOCK);
        RecipeBuilderShaped.shaped(Items.DIAMOND_BOOTS).define('X', Items.DIAMOND).pattern("X X").pattern("X X").unlockedBy("has_diamond", has(Items.DIAMOND)).save(exporter);
        RecipeBuilderShaped.shaped(Items.DIAMOND_CHESTPLATE).define('X', Items.DIAMOND).pattern("X X").pattern("XXX").pattern("XXX").unlockedBy("has_diamond", has(Items.DIAMOND)).save(exporter);
        RecipeBuilderShaped.shaped(Items.DIAMOND_HELMET).define('X', Items.DIAMOND).pattern("XXX").pattern("X X").unlockedBy("has_diamond", has(Items.DIAMOND)).save(exporter);
        RecipeBuilderShaped.shaped(Items.DIAMOND_HOE).define('#', Items.STICK).define('X', Items.DIAMOND).pattern("XX").pattern(" #").pattern(" #").unlockedBy("has_diamond", has(Items.DIAMOND)).save(exporter);
        RecipeBuilderShaped.shaped(Items.DIAMOND_LEGGINGS).define('X', Items.DIAMOND).pattern("XXX").pattern("X X").pattern("X X").unlockedBy("has_diamond", has(Items.DIAMOND)).save(exporter);
        RecipeBuilderShaped.shaped(Items.DIAMOND_PICKAXE).define('#', Items.STICK).define('X', Items.DIAMOND).pattern("XXX").pattern(" # ").pattern(" # ").unlockedBy("has_diamond", has(Items.DIAMOND)).save(exporter);
        RecipeBuilderShaped.shaped(Items.DIAMOND_SHOVEL).define('#', Items.STICK).define('X', Items.DIAMOND).pattern("X").pattern("#").pattern("#").unlockedBy("has_diamond", has(Items.DIAMOND)).save(exporter);
        RecipeBuilderShaped.shaped(Items.DIAMOND_SWORD).define('#', Items.STICK).define('X', Items.DIAMOND).pattern("X").pattern("X").pattern("#").unlockedBy("has_diamond", has(Items.DIAMOND)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.DIORITE, 2).define('Q', Items.QUARTZ).define('C', Blocks.COBBLESTONE).pattern("CQ").pattern("QC").unlockedBy("has_quartz", has(Items.QUARTZ)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.DISPENSER).define('R', Items.REDSTONE).define('#', Blocks.COBBLESTONE).define('X', Items.BOW).pattern("###").pattern("#X#").pattern("#R#").unlockedBy("has_bow", has(Items.BOW)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.DRIPSTONE_BLOCK).define('#', Items.POINTED_DRIPSTONE).pattern("##").pattern("##").group("pointed_dripstone").unlockedBy("has_pointed_dripstone", has(Items.POINTED_DRIPSTONE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.DROPPER).define('R', Items.REDSTONE).define('#', Blocks.COBBLESTONE).pattern("###").pattern("# #").pattern("#R#").unlockedBy("has_redstone", has(Items.REDSTONE)).save(exporter);
        nineBlockStorageRecipes(exporter, Items.EMERALD, Items.EMERALD_BLOCK);
        RecipeBuilderShaped.shaped(Blocks.ENCHANTING_TABLE).define('B', Items.BOOK).define('#', Blocks.OBSIDIAN).define('D', Items.DIAMOND).pattern(" B ").pattern("D#D").pattern("###").unlockedBy("has_obsidian", has(Blocks.OBSIDIAN)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.ENDER_CHEST).define('#', Blocks.OBSIDIAN).define('E', Items.ENDER_EYE).pattern("###").pattern("#E#").pattern("###").unlockedBy("has_ender_eye", has(Items.ENDER_EYE)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.ENDER_EYE).requires(Items.ENDER_PEARL).requires(Items.BLAZE_POWDER).unlockedBy("has_blaze_powder", has(Items.BLAZE_POWDER)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.END_STONE_BRICKS, 4).define('#', Blocks.END_STONE).pattern("##").pattern("##").unlockedBy("has_end_stone", has(Blocks.END_STONE)).save(exporter);
        RecipeBuilderShaped.shaped(Items.END_CRYSTAL).define('T', Items.GHAST_TEAR).define('E', Items.ENDER_EYE).define('G', Blocks.GLASS).pattern("GGG").pattern("GEG").pattern("GTG").unlockedBy("has_ender_eye", has(Items.ENDER_EYE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.END_ROD, 4).define('#', Items.POPPED_CHORUS_FRUIT).define('/', Items.BLAZE_ROD).pattern("/").pattern("#").unlockedBy("has_chorus_fruit_popped", has(Items.POPPED_CHORUS_FRUIT)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.FERMENTED_SPIDER_EYE).requires(Items.SPIDER_EYE).requires(Blocks.BROWN_MUSHROOM).requires(Items.SUGAR).unlockedBy("has_spider_eye", has(Items.SPIDER_EYE)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.FIRE_CHARGE, 3).requires(Items.GUNPOWDER).requires(Items.BLAZE_POWDER).requires(RecipeItemStack.of(Items.COAL, Items.CHARCOAL)).unlockedBy("has_blaze_powder", has(Items.BLAZE_POWDER)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.FIREWORK_ROCKET, 3).requires(Items.GUNPOWDER).requires(Items.PAPER).unlockedBy("has_gunpowder", has(Items.GUNPOWDER)).save(exporter, "firework_rocket_simple");
        RecipeBuilderShaped.shaped(Items.FISHING_ROD).define('#', Items.STICK).define('X', Items.STRING).pattern("  #").pattern(" #X").pattern("# X").unlockedBy("has_string", has(Items.STRING)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.FLINT_AND_STEEL).requires(Items.IRON_INGOT).requires(Items.FLINT).unlockedBy("has_flint", has(Items.FLINT)).unlockedBy("has_obsidian", has(Blocks.OBSIDIAN)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.FLOWER_POT).define('#', Items.BRICK).pattern("# #").pattern(" # ").unlockedBy("has_brick", has(Items.BRICK)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.FURNACE).define('#', TagsItem.STONE_CRAFTING_MATERIALS).pattern("###").pattern("# #").pattern("###").unlockedBy("has_cobblestone", has(TagsItem.STONE_CRAFTING_MATERIALS)).save(exporter);
        RecipeBuilderShaped.shaped(Items.FURNACE_MINECART).define('A', Blocks.FURNACE).define('B', Items.MINECART).pattern("A").pattern("B").unlockedBy("has_minecart", has(Items.MINECART)).save(exporter);
        RecipeBuilderShaped.shaped(Items.GLASS_BOTTLE, 3).define('#', Blocks.GLASS).pattern("# #").pattern(" # ").unlockedBy("has_glass", has(Blocks.GLASS)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.GLASS_PANE, 16).define('#', Blocks.GLASS).pattern("###").pattern("###").unlockedBy("has_glass", has(Blocks.GLASS)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.GLOWSTONE).define('#', Items.GLOWSTONE_DUST).pattern("##").pattern("##").unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.GLOW_ITEM_FRAME).requires(Items.ITEM_FRAME).requires(Items.GLOW_INK_SAC).unlockedBy("has_item_frame", has(Items.ITEM_FRAME)).unlockedBy("has_glow_ink_sac", has(Items.GLOW_INK_SAC)).save(exporter);
        RecipeBuilderShaped.shaped(Items.GOLDEN_APPLE).define('#', Items.GOLD_INGOT).define('X', Items.APPLE).pattern("###").pattern("#X#").pattern("###").unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.GOLDEN_AXE).define('#', Items.STICK).define('X', Items.GOLD_INGOT).pattern("XX").pattern("X#").pattern(" #").unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.GOLDEN_BOOTS).define('X', Items.GOLD_INGOT).pattern("X X").pattern("X X").unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.GOLDEN_CARROT).define('#', Items.GOLD_NUGGET).define('X', Items.CARROT).pattern("###").pattern("#X#").pattern("###").unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET)).save(exporter);
        RecipeBuilderShaped.shaped(Items.GOLDEN_CHESTPLATE).define('X', Items.GOLD_INGOT).pattern("X X").pattern("XXX").pattern("XXX").unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.GOLDEN_HELMET).define('X', Items.GOLD_INGOT).pattern("XXX").pattern("X X").unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.GOLDEN_HOE).define('#', Items.STICK).define('X', Items.GOLD_INGOT).pattern("XX").pattern(" #").pattern(" #").unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.GOLDEN_LEGGINGS).define('X', Items.GOLD_INGOT).pattern("XXX").pattern("X X").pattern("X X").unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.GOLDEN_PICKAXE).define('#', Items.STICK).define('X', Items.GOLD_INGOT).pattern("XXX").pattern(" # ").pattern(" # ").unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.POWERED_RAIL, 6).define('R', Items.REDSTONE).define('#', Items.STICK).define('X', Items.GOLD_INGOT).pattern("X X").pattern("X#X").pattern("XRX").unlockedBy("has_rail", has(Blocks.RAIL)).save(exporter);
        RecipeBuilderShaped.shaped(Items.GOLDEN_SHOVEL).define('#', Items.STICK).define('X', Items.GOLD_INGOT).pattern("X").pattern("#").pattern("#").unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.GOLDEN_SWORD).define('#', Items.STICK).define('X', Items.GOLD_INGOT).pattern("X").pattern("X").pattern("#").unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT)).save(exporter);
        nineBlockStorageRecipesRecipesWithCustomUnpacking(exporter, Items.GOLD_INGOT, Items.GOLD_BLOCK, "gold_ingot_from_gold_block", "gold_ingot");
        nineBlockStorageRecipesWithCustomPacking(exporter, Items.GOLD_NUGGET, Items.GOLD_INGOT, "gold_ingot_from_nuggets", "gold_ingot");
        RecipeBuilderShapeless.shapeless(Blocks.GRANITE).requires(Blocks.DIORITE).requires(Items.QUARTZ).unlockedBy("has_quartz", has(Items.QUARTZ)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.GRAY_DYE, 2).requires(Items.BLACK_DYE).requires(Items.WHITE_DYE).unlockedBy("has_white_dye", has(Items.WHITE_DYE)).unlockedBy("has_black_dye", has(Items.BLACK_DYE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.HAY_BLOCK).define('#', Items.WHEAT).pattern("###").pattern("###").pattern("###").unlockedBy("has_wheat", has(Items.WHEAT)).save(exporter);
        pressurePlate(exporter, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Items.IRON_INGOT);
        RecipeBuilderShapeless.shapeless(Items.HONEY_BOTTLE, 4).requires(Items.HONEY_BLOCK).requires(Items.GLASS_BOTTLE, 4).unlockedBy("has_honey_block", has(Blocks.HONEY_BLOCK)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.HONEY_BLOCK, 1).define('S', Items.HONEY_BOTTLE).pattern("SS").pattern("SS").unlockedBy("has_honey_bottle", has(Items.HONEY_BOTTLE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.HONEYCOMB_BLOCK).define('H', Items.HONEYCOMB).pattern("HH").pattern("HH").unlockedBy("has_honeycomb", has(Items.HONEYCOMB)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.HOPPER).define('C', Blocks.CHEST).define('I', Items.IRON_INGOT).pattern("I I").pattern("ICI").pattern(" I ").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.HOPPER_MINECART).define('A', Blocks.HOPPER).define('B', Items.MINECART).pattern("A").pattern("B").unlockedBy("has_minecart", has(Items.MINECART)).save(exporter);
        RecipeBuilderShaped.shaped(Items.IRON_AXE).define('#', Items.STICK).define('X', Items.IRON_INGOT).pattern("XX").pattern("X#").pattern(" #").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.IRON_BARS, 16).define('#', Items.IRON_INGOT).pattern("###").pattern("###").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.IRON_BOOTS).define('X', Items.IRON_INGOT).pattern("X X").pattern("X X").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.IRON_CHESTPLATE).define('X', Items.IRON_INGOT).pattern("X X").pattern("XXX").pattern("XXX").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        doorBuilder(Blocks.IRON_DOOR, RecipeItemStack.of(Items.IRON_INGOT)).unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.IRON_HELMET).define('X', Items.IRON_INGOT).pattern("XXX").pattern("X X").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.IRON_HOE).define('#', Items.STICK).define('X', Items.IRON_INGOT).pattern("XX").pattern(" #").pattern(" #").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        nineBlockStorageRecipesRecipesWithCustomUnpacking(exporter, Items.IRON_INGOT, Items.IRON_BLOCK, "iron_ingot_from_iron_block", "iron_ingot");
        nineBlockStorageRecipesWithCustomPacking(exporter, Items.IRON_NUGGET, Items.IRON_INGOT, "iron_ingot_from_nuggets", "iron_ingot");
        RecipeBuilderShaped.shaped(Items.IRON_LEGGINGS).define('X', Items.IRON_INGOT).pattern("XXX").pattern("X X").pattern("X X").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.IRON_PICKAXE).define('#', Items.STICK).define('X', Items.IRON_INGOT).pattern("XXX").pattern(" # ").pattern(" # ").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.IRON_SHOVEL).define('#', Items.STICK).define('X', Items.IRON_INGOT).pattern("X").pattern("#").pattern("#").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.IRON_SWORD).define('#', Items.STICK).define('X', Items.IRON_INGOT).pattern("X").pattern("X").pattern("#").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.IRON_TRAPDOOR).define('#', Items.IRON_INGOT).pattern("##").pattern("##").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.ITEM_FRAME).define('#', Items.STICK).define('X', Items.LEATHER).pattern("###").pattern("#X#").pattern("###").unlockedBy("has_leather", has(Items.LEATHER)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.JUKEBOX).define('#', TagsItem.PLANKS).define('X', Items.DIAMOND).pattern("###").pattern("#X#").pattern("###").unlockedBy("has_diamond", has(Items.DIAMOND)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.LADDER, 3).define('#', Items.STICK).pattern("# #").pattern("###").pattern("# #").unlockedBy("has_stick", has(Items.STICK)).save(exporter);
        nineBlockStorageRecipes(exporter, Items.LAPIS_LAZULI, Items.LAPIS_BLOCK);
        RecipeBuilderShaped.shaped(Items.LEAD, 2).define('~', Items.STRING).define('O', Items.SLIME_BALL).pattern("~~ ").pattern("~O ").pattern("  ~").unlockedBy("has_slime_ball", has(Items.SLIME_BALL)).save(exporter);
        RecipeBuilderShaped.shaped(Items.LEATHER).define('#', Items.RABBIT_HIDE).pattern("##").pattern("##").unlockedBy("has_rabbit_hide", has(Items.RABBIT_HIDE)).save(exporter);
        RecipeBuilderShaped.shaped(Items.LEATHER_BOOTS).define('X', Items.LEATHER).pattern("X X").pattern("X X").unlockedBy("has_leather", has(Items.LEATHER)).save(exporter);
        RecipeBuilderShaped.shaped(Items.LEATHER_CHESTPLATE).define('X', Items.LEATHER).pattern("X X").pattern("XXX").pattern("XXX").unlockedBy("has_leather", has(Items.LEATHER)).save(exporter);
        RecipeBuilderShaped.shaped(Items.LEATHER_HELMET).define('X', Items.LEATHER).pattern("XXX").pattern("X X").unlockedBy("has_leather", has(Items.LEATHER)).save(exporter);
        RecipeBuilderShaped.shaped(Items.LEATHER_LEGGINGS).define('X', Items.LEATHER).pattern("XXX").pattern("X X").pattern("X X").unlockedBy("has_leather", has(Items.LEATHER)).save(exporter);
        RecipeBuilderShaped.shaped(Items.LEATHER_HORSE_ARMOR).define('X', Items.LEATHER).pattern("X X").pattern("XXX").pattern("X X").unlockedBy("has_leather", has(Items.LEATHER)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.LECTERN).define('S', TagsItem.WOODEN_SLABS).define('B', Blocks.BOOKSHELF).pattern("SSS").pattern(" B ").pattern(" S ").unlockedBy("has_book", has(Items.BOOK)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.LEVER).define('#', Blocks.COBBLESTONE).define('X', Items.STICK).pattern("X").pattern("#").unlockedBy("has_cobblestone", has(Blocks.COBBLESTONE)).save(exporter);
        oneToOneConversionRecipe(exporter, Items.LIGHT_BLUE_DYE, Blocks.BLUE_ORCHID, "light_blue_dye");
        RecipeBuilderShapeless.shapeless(Items.LIGHT_BLUE_DYE, 2).requires(Items.BLUE_DYE).requires(Items.WHITE_DYE).group("light_blue_dye").unlockedBy("has_blue_dye", has(Items.BLUE_DYE)).unlockedBy("has_white_dye", has(Items.WHITE_DYE)).save(exporter, "light_blue_dye_from_blue_white_dye");
        oneToOneConversionRecipe(exporter, Items.LIGHT_GRAY_DYE, Blocks.AZURE_BLUET, "light_gray_dye");
        RecipeBuilderShapeless.shapeless(Items.LIGHT_GRAY_DYE, 2).requires(Items.GRAY_DYE).requires(Items.WHITE_DYE).group("light_gray_dye").unlockedBy("has_gray_dye", has(Items.GRAY_DYE)).unlockedBy("has_white_dye", has(Items.WHITE_DYE)).save(exporter, "light_gray_dye_from_gray_white_dye");
        RecipeBuilderShapeless.shapeless(Items.LIGHT_GRAY_DYE, 3).requires(Items.BLACK_DYE).requires(Items.WHITE_DYE, 2).group("light_gray_dye").unlockedBy("has_white_dye", has(Items.WHITE_DYE)).unlockedBy("has_black_dye", has(Items.BLACK_DYE)).save(exporter, "light_gray_dye_from_black_white_dye");
        oneToOneConversionRecipe(exporter, Items.LIGHT_GRAY_DYE, Blocks.OXEYE_DAISY, "light_gray_dye");
        oneToOneConversionRecipe(exporter, Items.LIGHT_GRAY_DYE, Blocks.WHITE_TULIP, "light_gray_dye");
        pressurePlate(exporter, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.GOLD_INGOT);
        RecipeBuilderShaped.shaped(Blocks.LIGHTNING_ROD).define('#', Items.COPPER_INGOT).pattern("#").pattern("#").pattern("#").unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.LIME_DYE, 2).requires(Items.GREEN_DYE).requires(Items.WHITE_DYE).unlockedBy("has_green_dye", has(Items.GREEN_DYE)).unlockedBy("has_white_dye", has(Items.WHITE_DYE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.JACK_O_LANTERN).define('A', Blocks.CARVED_PUMPKIN).define('B', Blocks.TORCH).pattern("A").pattern("B").unlockedBy("has_carved_pumpkin", has(Blocks.CARVED_PUMPKIN)).save(exporter);
        oneToOneConversionRecipe(exporter, Items.MAGENTA_DYE, Blocks.ALLIUM, "magenta_dye");
        RecipeBuilderShapeless.shapeless(Items.MAGENTA_DYE, 4).requires(Items.BLUE_DYE).requires(Items.RED_DYE, 2).requires(Items.WHITE_DYE).group("magenta_dye").unlockedBy("has_blue_dye", has(Items.BLUE_DYE)).unlockedBy("has_rose_red", has(Items.RED_DYE)).unlockedBy("has_white_dye", has(Items.WHITE_DYE)).save(exporter, "magenta_dye_from_blue_red_white_dye");
        RecipeBuilderShapeless.shapeless(Items.MAGENTA_DYE, 3).requires(Items.BLUE_DYE).requires(Items.RED_DYE).requires(Items.PINK_DYE).group("magenta_dye").unlockedBy("has_pink_dye", has(Items.PINK_DYE)).unlockedBy("has_blue_dye", has(Items.BLUE_DYE)).unlockedBy("has_red_dye", has(Items.RED_DYE)).save(exporter, "magenta_dye_from_blue_red_pink");
        oneToOneConversionRecipe(exporter, Items.MAGENTA_DYE, Blocks.LILAC, "magenta_dye", 2);
        RecipeBuilderShapeless.shapeless(Items.MAGENTA_DYE, 2).requires(Items.PURPLE_DYE).requires(Items.PINK_DYE).group("magenta_dye").unlockedBy("has_pink_dye", has(Items.PINK_DYE)).unlockedBy("has_purple_dye", has(Items.PURPLE_DYE)).save(exporter, "magenta_dye_from_purple_and_pink");
        RecipeBuilderShaped.shaped(Blocks.MAGMA_BLOCK).define('#', Items.MAGMA_CREAM).pattern("##").pattern("##").unlockedBy("has_magma_cream", has(Items.MAGMA_CREAM)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.MAGMA_CREAM).requires(Items.BLAZE_POWDER).requires(Items.SLIME_BALL).unlockedBy("has_blaze_powder", has(Items.BLAZE_POWDER)).save(exporter);
        RecipeBuilderShaped.shaped(Items.MAP).define('#', Items.PAPER).define('X', Items.COMPASS).pattern("###").pattern("#X#").pattern("###").unlockedBy("has_compass", has(Items.COMPASS)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.MELON).define('M', Items.MELON_SLICE).pattern("MMM").pattern("MMM").pattern("MMM").unlockedBy("has_melon", has(Items.MELON_SLICE)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.MELON_SEEDS).requires(Items.MELON_SLICE).unlockedBy("has_melon", has(Items.MELON_SLICE)).save(exporter);
        RecipeBuilderShaped.shaped(Items.MINECART).define('#', Items.IRON_INGOT).pattern("# #").pattern("###").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShapeless.shapeless(Blocks.MOSSY_COBBLESTONE).requires(Blocks.COBBLESTONE).requires(Blocks.VINE).group("mossy_cobblestone").unlockedBy("has_vine", has(Blocks.VINE)).save(exporter, getConversionRecipeName(Blocks.MOSSY_COBBLESTONE, Blocks.VINE));
        RecipeBuilderShapeless.shapeless(Blocks.MOSSY_STONE_BRICKS).requires(Blocks.STONE_BRICKS).requires(Blocks.VINE).group("mossy_stone_bricks").unlockedBy("has_vine", has(Blocks.VINE)).save(exporter, getConversionRecipeName(Blocks.MOSSY_STONE_BRICKS, Blocks.VINE));
        RecipeBuilderShapeless.shapeless(Blocks.MOSSY_COBBLESTONE).requires(Blocks.COBBLESTONE).requires(Blocks.MOSS_BLOCK).group("mossy_cobblestone").unlockedBy("has_moss_block", has(Blocks.MOSS_BLOCK)).save(exporter, getConversionRecipeName(Blocks.MOSSY_COBBLESTONE, Blocks.MOSS_BLOCK));
        RecipeBuilderShapeless.shapeless(Blocks.MOSSY_STONE_BRICKS).requires(Blocks.STONE_BRICKS).requires(Blocks.MOSS_BLOCK).group("mossy_stone_bricks").unlockedBy("has_moss_block", has(Blocks.MOSS_BLOCK)).save(exporter, getConversionRecipeName(Blocks.MOSSY_STONE_BRICKS, Blocks.MOSS_BLOCK));
        RecipeBuilderShapeless.shapeless(Items.MUSHROOM_STEW).requires(Blocks.BROWN_MUSHROOM).requires(Blocks.RED_MUSHROOM).requires(Items.BOWL).unlockedBy("has_mushroom_stew", has(Items.MUSHROOM_STEW)).unlockedBy("has_bowl", has(Items.BOWL)).unlockedBy("has_brown_mushroom", has(Blocks.BROWN_MUSHROOM)).unlockedBy("has_red_mushroom", has(Blocks.RED_MUSHROOM)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.NETHER_BRICKS).define('N', Items.NETHER_BRICK).pattern("NN").pattern("NN").unlockedBy("has_netherbrick", has(Items.NETHER_BRICK)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.NETHER_WART_BLOCK).define('#', Items.NETHER_WART).pattern("###").pattern("###").pattern("###").unlockedBy("has_nether_wart", has(Items.NETHER_WART)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.NOTE_BLOCK).define('#', TagsItem.PLANKS).define('X', Items.REDSTONE).pattern("###").pattern("#X#").pattern("###").unlockedBy("has_redstone", has(Items.REDSTONE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.OBSERVER).define('Q', Items.QUARTZ).define('R', Items.REDSTONE).define('#', Blocks.COBBLESTONE).pattern("###").pattern("RRQ").pattern("###").unlockedBy("has_quartz", has(Items.QUARTZ)).save(exporter);
        oneToOneConversionRecipe(exporter, Items.ORANGE_DYE, Blocks.ORANGE_TULIP, "orange_dye");
        RecipeBuilderShapeless.shapeless(Items.ORANGE_DYE, 2).requires(Items.RED_DYE).requires(Items.YELLOW_DYE).group("orange_dye").unlockedBy("has_red_dye", has(Items.RED_DYE)).unlockedBy("has_yellow_dye", has(Items.YELLOW_DYE)).save(exporter, "orange_dye_from_red_yellow");
        RecipeBuilderShaped.shaped(Items.PAINTING).define('#', Items.STICK).define('X', RecipeItemStack.of(TagsItem.WOOL)).pattern("###").pattern("#X#").pattern("###").unlockedBy("has_wool", has(TagsItem.WOOL)).save(exporter);
        RecipeBuilderShaped.shaped(Items.PAPER, 3).define('#', Blocks.SUGAR_CANE).pattern("###").unlockedBy("has_reeds", has(Blocks.SUGAR_CANE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.QUARTZ_PILLAR, 2).define('#', Blocks.QUARTZ_BLOCK).pattern("#").pattern("#").unlockedBy("has_chiseled_quartz_block", has(Blocks.CHISELED_QUARTZ_BLOCK)).unlockedBy("has_quartz_block", has(Blocks.QUARTZ_BLOCK)).unlockedBy("has_quartz_pillar", has(Blocks.QUARTZ_PILLAR)).save(exporter);
        RecipeBuilderShapeless.shapeless(Blocks.PACKED_ICE).requires(Blocks.ICE, 9).unlockedBy("has_ice", has(Blocks.ICE)).save(exporter);
        oneToOneConversionRecipe(exporter, Items.PINK_DYE, Blocks.PEONY, "pink_dye", 2);
        oneToOneConversionRecipe(exporter, Items.PINK_DYE, Blocks.PINK_TULIP, "pink_dye");
        RecipeBuilderShapeless.shapeless(Items.PINK_DYE, 2).requires(Items.RED_DYE).requires(Items.WHITE_DYE).group("pink_dye").unlockedBy("has_white_dye", has(Items.WHITE_DYE)).unlockedBy("has_red_dye", has(Items.RED_DYE)).save(exporter, "pink_dye_from_red_white_dye");
        RecipeBuilderShaped.shaped(Blocks.PISTON).define('R', Items.REDSTONE).define('#', Blocks.COBBLESTONE).define('T', TagsItem.PLANKS).define('X', Items.IRON_INGOT).pattern("TTT").pattern("#X#").pattern("#R#").unlockedBy("has_redstone", has(Items.REDSTONE)).save(exporter);
        polished(exporter, Blocks.POLISHED_BASALT, Blocks.BASALT);
        RecipeBuilderShaped.shaped(Blocks.PRISMARINE).define('S', Items.PRISMARINE_SHARD).pattern("SS").pattern("SS").unlockedBy("has_prismarine_shard", has(Items.PRISMARINE_SHARD)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.PRISMARINE_BRICKS).define('S', Items.PRISMARINE_SHARD).pattern("SSS").pattern("SSS").pattern("SSS").unlockedBy("has_prismarine_shard", has(Items.PRISMARINE_SHARD)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.PUMPKIN_PIE).requires(Blocks.PUMPKIN).requires(Items.SUGAR).requires(Items.EGG).unlockedBy("has_carved_pumpkin", has(Blocks.CARVED_PUMPKIN)).unlockedBy("has_pumpkin", has(Blocks.PUMPKIN)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.PUMPKIN_SEEDS, 4).requires(Blocks.PUMPKIN).unlockedBy("has_pumpkin", has(Blocks.PUMPKIN)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.PURPLE_DYE, 2).requires(Items.BLUE_DYE).requires(Items.RED_DYE).unlockedBy("has_blue_dye", has(Items.BLUE_DYE)).unlockedBy("has_red_dye", has(Items.RED_DYE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.SHULKER_BOX).define('#', Blocks.CHEST).define('-', Items.SHULKER_SHELL).pattern("-").pattern("#").pattern("-").unlockedBy("has_shulker_shell", has(Items.SHULKER_SHELL)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.PURPUR_BLOCK, 4).define('F', Items.POPPED_CHORUS_FRUIT).pattern("FF").pattern("FF").unlockedBy("has_chorus_fruit_popped", has(Items.POPPED_CHORUS_FRUIT)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.PURPUR_PILLAR).define('#', Blocks.PURPUR_SLAB).pattern("#").pattern("#").unlockedBy("has_purpur_block", has(Blocks.PURPUR_BLOCK)).save(exporter);
        slabBuilder(Blocks.PURPUR_SLAB, RecipeItemStack.of(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR)).unlockedBy("has_purpur_block", has(Blocks.PURPUR_BLOCK)).save(exporter);
        stairBuilder(Blocks.PURPUR_STAIRS, RecipeItemStack.of(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR)).unlockedBy("has_purpur_block", has(Blocks.PURPUR_BLOCK)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.QUARTZ_BLOCK).define('#', Items.QUARTZ).pattern("##").pattern("##").unlockedBy("has_quartz", has(Items.QUARTZ)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.QUARTZ_BRICKS, 4).define('#', Blocks.QUARTZ_BLOCK).pattern("##").pattern("##").unlockedBy("has_quartz_block", has(Blocks.QUARTZ_BLOCK)).save(exporter);
        slabBuilder(Blocks.QUARTZ_SLAB, RecipeItemStack.of(Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR)).unlockedBy("has_chiseled_quartz_block", has(Blocks.CHISELED_QUARTZ_BLOCK)).unlockedBy("has_quartz_block", has(Blocks.QUARTZ_BLOCK)).unlockedBy("has_quartz_pillar", has(Blocks.QUARTZ_PILLAR)).save(exporter);
        stairBuilder(Blocks.QUARTZ_STAIRS, RecipeItemStack.of(Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR)).unlockedBy("has_chiseled_quartz_block", has(Blocks.CHISELED_QUARTZ_BLOCK)).unlockedBy("has_quartz_block", has(Blocks.QUARTZ_BLOCK)).unlockedBy("has_quartz_pillar", has(Blocks.QUARTZ_PILLAR)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.RABBIT_STEW).requires(Items.BAKED_POTATO).requires(Items.COOKED_RABBIT).requires(Items.BOWL).requires(Items.CARROT).requires(Blocks.BROWN_MUSHROOM).group("rabbit_stew").unlockedBy("has_cooked_rabbit", has(Items.COOKED_RABBIT)).save(exporter, getConversionRecipeName(Items.RABBIT_STEW, Items.BROWN_MUSHROOM));
        RecipeBuilderShapeless.shapeless(Items.RABBIT_STEW).requires(Items.BAKED_POTATO).requires(Items.COOKED_RABBIT).requires(Items.BOWL).requires(Items.CARROT).requires(Blocks.RED_MUSHROOM).group("rabbit_stew").unlockedBy("has_cooked_rabbit", has(Items.COOKED_RABBIT)).save(exporter, getConversionRecipeName(Items.RABBIT_STEW, Items.RED_MUSHROOM));
        RecipeBuilderShaped.shaped(Blocks.RAIL, 16).define('#', Items.STICK).define('X', Items.IRON_INGOT).pattern("X X").pattern("X#X").pattern("X X").unlockedBy("has_minecart", has(Items.MINECART)).save(exporter);
        nineBlockStorageRecipes(exporter, Items.REDSTONE, Items.REDSTONE_BLOCK);
        RecipeBuilderShaped.shaped(Blocks.REDSTONE_LAMP).define('R', Items.REDSTONE).define('G', Blocks.GLOWSTONE).pattern(" R ").pattern("RGR").pattern(" R ").unlockedBy("has_glowstone", has(Blocks.GLOWSTONE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.REDSTONE_TORCH).define('#', Items.STICK).define('X', Items.REDSTONE).pattern("X").pattern("#").unlockedBy("has_redstone", has(Items.REDSTONE)).save(exporter);
        oneToOneConversionRecipe(exporter, Items.RED_DYE, Items.BEETROOT, "red_dye");
        oneToOneConversionRecipe(exporter, Items.RED_DYE, Blocks.POPPY, "red_dye");
        oneToOneConversionRecipe(exporter, Items.RED_DYE, Blocks.ROSE_BUSH, "red_dye", 2);
        RecipeBuilderShapeless.shapeless(Items.RED_DYE).requires(Blocks.RED_TULIP).group("red_dye").unlockedBy("has_red_flower", has(Blocks.RED_TULIP)).save(exporter, "red_dye_from_tulip");
        RecipeBuilderShaped.shaped(Blocks.RED_NETHER_BRICKS).define('W', Items.NETHER_WART).define('N', Items.NETHER_BRICK).pattern("NW").pattern("WN").unlockedBy("has_nether_wart", has(Items.NETHER_WART)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.RED_SANDSTONE).define('#', Blocks.RED_SAND).pattern("##").pattern("##").unlockedBy("has_sand", has(Blocks.RED_SAND)).save(exporter);
        slabBuilder(Blocks.RED_SANDSTONE_SLAB, RecipeItemStack.of(Blocks.RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE)).unlockedBy("has_red_sandstone", has(Blocks.RED_SANDSTONE)).unlockedBy("has_chiseled_red_sandstone", has(Blocks.CHISELED_RED_SANDSTONE)).save(exporter);
        stairBuilder(Blocks.RED_SANDSTONE_STAIRS, RecipeItemStack.of(Blocks.RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE)).unlockedBy("has_red_sandstone", has(Blocks.RED_SANDSTONE)).unlockedBy("has_chiseled_red_sandstone", has(Blocks.CHISELED_RED_SANDSTONE)).unlockedBy("has_cut_red_sandstone", has(Blocks.CUT_RED_SANDSTONE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.REPEATER).define('#', Blocks.REDSTONE_TORCH).define('X', Items.REDSTONE).define('I', Blocks.STONE).pattern("#X#").pattern("III").unlockedBy("has_redstone_torch", has(Blocks.REDSTONE_TORCH)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.SANDSTONE).define('#', Blocks.SAND).pattern("##").pattern("##").unlockedBy("has_sand", has(Blocks.SAND)).save(exporter);
        slabBuilder(Blocks.SANDSTONE_SLAB, RecipeItemStack.of(Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE)).unlockedBy("has_sandstone", has(Blocks.SANDSTONE)).unlockedBy("has_chiseled_sandstone", has(Blocks.CHISELED_SANDSTONE)).save(exporter);
        stairBuilder(Blocks.SANDSTONE_STAIRS, RecipeItemStack.of(Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.CUT_SANDSTONE)).unlockedBy("has_sandstone", has(Blocks.SANDSTONE)).unlockedBy("has_chiseled_sandstone", has(Blocks.CHISELED_SANDSTONE)).unlockedBy("has_cut_sandstone", has(Blocks.CUT_SANDSTONE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.SEA_LANTERN).define('S', Items.PRISMARINE_SHARD).define('C', Items.PRISMARINE_CRYSTALS).pattern("SCS").pattern("CCC").pattern("SCS").unlockedBy("has_prismarine_crystals", has(Items.PRISMARINE_CRYSTALS)).save(exporter);
        RecipeBuilderShaped.shaped(Items.SHEARS).define('#', Items.IRON_INGOT).pattern(" #").pattern("# ").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Items.SHIELD).define('W', TagsItem.PLANKS).define('o', Items.IRON_INGOT).pattern("WoW").pattern("WWW").pattern(" W ").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        nineBlockStorageRecipes(exporter, Items.SLIME_BALL, Items.SLIME_BLOCK);
        cut(exporter, Blocks.CUT_RED_SANDSTONE, Blocks.RED_SANDSTONE);
        cut(exporter, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE);
        RecipeBuilderShaped.shaped(Blocks.SNOW_BLOCK).define('#', Items.SNOWBALL).pattern("##").pattern("##").unlockedBy("has_snowball", has(Items.SNOWBALL)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.SNOW, 6).define('#', Blocks.SNOW_BLOCK).pattern("###").unlockedBy("has_snowball", has(Items.SNOWBALL)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.SOUL_CAMPFIRE).define('L', TagsItem.LOGS).define('S', Items.STICK).define('#', TagsItem.SOUL_FIRE_BASE_BLOCKS).pattern(" S ").pattern("S#S").pattern("LLL").unlockedBy("has_stick", has(Items.STICK)).unlockedBy("has_soul_sand", has(TagsItem.SOUL_FIRE_BASE_BLOCKS)).save(exporter);
        RecipeBuilderShaped.shaped(Items.GLISTERING_MELON_SLICE).define('#', Items.GOLD_NUGGET).define('X', Items.MELON_SLICE).pattern("###").pattern("#X#").pattern("###").unlockedBy("has_melon", has(Items.MELON_SLICE)).save(exporter);
        RecipeBuilderShaped.shaped(Items.SPECTRAL_ARROW, 2).define('#', Items.GLOWSTONE_DUST).define('X', Items.ARROW).pattern(" # ").pattern("#X#").pattern(" # ").unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST)).save(exporter);
        RecipeBuilderShaped.shaped(Items.SPYGLASS).define('#', Items.AMETHYST_SHARD).define('X', Items.COPPER_INGOT).pattern(" # ").pattern(" X ").pattern(" X ").unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD)).save(exporter);
        RecipeBuilderShaped.shaped(Items.STICK, 4).define('#', TagsItem.PLANKS).pattern("#").pattern("#").group("sticks").unlockedBy("has_planks", has(TagsItem.PLANKS)).save(exporter);
        RecipeBuilderShaped.shaped(Items.STICK, 1).define('#', Blocks.BAMBOO).pattern("#").pattern("#").group("sticks").unlockedBy("has_bamboo", has(Blocks.BAMBOO)).save(exporter, "stick_from_bamboo_item");
        RecipeBuilderShaped.shaped(Blocks.STICKY_PISTON).define('P', Blocks.PISTON).define('S', Items.SLIME_BALL).pattern("S").pattern("P").unlockedBy("has_slime_ball", has(Items.SLIME_BALL)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.STONE_BRICKS, 4).define('#', Blocks.STONE).pattern("##").pattern("##").unlockedBy("has_stone", has(Blocks.STONE)).save(exporter);
        RecipeBuilderShaped.shaped(Items.STONE_AXE).define('#', Items.STICK).define('X', TagsItem.STONE_TOOL_MATERIALS).pattern("XX").pattern("X#").pattern(" #").unlockedBy("has_cobblestone", has(TagsItem.STONE_TOOL_MATERIALS)).save(exporter);
        slabBuilder(Blocks.STONE_BRICK_SLAB, RecipeItemStack.of(Blocks.STONE_BRICKS)).unlockedBy("has_stone_bricks", has(TagsItem.STONE_BRICKS)).save(exporter);
        stairBuilder(Blocks.STONE_BRICK_STAIRS, RecipeItemStack.of(Blocks.STONE_BRICKS)).unlockedBy("has_stone_bricks", has(TagsItem.STONE_BRICKS)).save(exporter);
        RecipeBuilderShaped.shaped(Items.STONE_HOE).define('#', Items.STICK).define('X', TagsItem.STONE_TOOL_MATERIALS).pattern("XX").pattern(" #").pattern(" #").unlockedBy("has_cobblestone", has(TagsItem.STONE_TOOL_MATERIALS)).save(exporter);
        RecipeBuilderShaped.shaped(Items.STONE_PICKAXE).define('#', Items.STICK).define('X', TagsItem.STONE_TOOL_MATERIALS).pattern("XXX").pattern(" # ").pattern(" # ").unlockedBy("has_cobblestone", has(TagsItem.STONE_TOOL_MATERIALS)).save(exporter);
        RecipeBuilderShaped.shaped(Items.STONE_SHOVEL).define('#', Items.STICK).define('X', TagsItem.STONE_TOOL_MATERIALS).pattern("X").pattern("#").pattern("#").unlockedBy("has_cobblestone", has(TagsItem.STONE_TOOL_MATERIALS)).save(exporter);
        slab(exporter, Blocks.SMOOTH_STONE_SLAB, Blocks.SMOOTH_STONE);
        RecipeBuilderShaped.shaped(Items.STONE_SWORD).define('#', Items.STICK).define('X', TagsItem.STONE_TOOL_MATERIALS).pattern("X").pattern("X").pattern("#").unlockedBy("has_cobblestone", has(TagsItem.STONE_TOOL_MATERIALS)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.WHITE_WOOL).define('#', Items.STRING).pattern("##").pattern("##").unlockedBy("has_string", has(Items.STRING)).save(exporter, getConversionRecipeName(Blocks.WHITE_WOOL, Items.STRING));
        oneToOneConversionRecipe(exporter, Items.SUGAR, Blocks.SUGAR_CANE, "sugar");
        RecipeBuilderShapeless.shapeless(Items.SUGAR, 3).requires(Items.HONEY_BOTTLE).group("sugar").unlockedBy("has_honey_bottle", has(Items.HONEY_BOTTLE)).save(exporter, getConversionRecipeName(Items.SUGAR, Items.HONEY_BOTTLE));
        RecipeBuilderShaped.shaped(Blocks.TARGET).define('H', Items.HAY_BLOCK).define('R', Items.REDSTONE).pattern(" R ").pattern("RHR").pattern(" R ").unlockedBy("has_redstone", has(Items.REDSTONE)).unlockedBy("has_hay_block", has(Blocks.HAY_BLOCK)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.TNT).define('#', RecipeItemStack.of(Blocks.SAND, Blocks.RED_SAND)).define('X', Items.GUNPOWDER).pattern("X#X").pattern("#X#").pattern("X#X").unlockedBy("has_gunpowder", has(Items.GUNPOWDER)).save(exporter);
        RecipeBuilderShaped.shaped(Items.TNT_MINECART).define('A', Blocks.TNT).define('B', Items.MINECART).pattern("A").pattern("B").unlockedBy("has_minecart", has(Items.MINECART)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.TORCH, 4).define('#', Items.STICK).define('X', RecipeItemStack.of(Items.COAL, Items.CHARCOAL)).pattern("X").pattern("#").unlockedBy("has_stone_pickaxe", has(Items.STONE_PICKAXE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.SOUL_TORCH, 4).define('X', RecipeItemStack.of(Items.COAL, Items.CHARCOAL)).define('#', Items.STICK).define('S', TagsItem.SOUL_FIRE_BASE_BLOCKS).pattern("X").pattern("#").pattern("S").unlockedBy("has_soul_sand", has(TagsItem.SOUL_FIRE_BASE_BLOCKS)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.LANTERN).define('#', Items.TORCH).define('X', Items.IRON_NUGGET).pattern("XXX").pattern("X#X").pattern("XXX").unlockedBy("has_iron_nugget", has(Items.IRON_NUGGET)).unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.SOUL_LANTERN).define('#', Items.SOUL_TORCH).define('X', Items.IRON_NUGGET).pattern("XXX").pattern("X#X").pattern("XXX").unlockedBy("has_soul_torch", has(Items.SOUL_TORCH)).save(exporter);
        RecipeBuilderShapeless.shapeless(Blocks.TRAPPED_CHEST).requires(Blocks.CHEST).requires(Blocks.TRIPWIRE_HOOK).unlockedBy("has_tripwire_hook", has(Blocks.TRIPWIRE_HOOK)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.TRIPWIRE_HOOK, 2).define('#', TagsItem.PLANKS).define('S', Items.STICK).define('I', Items.IRON_INGOT).pattern("I").pattern("S").pattern("#").unlockedBy("has_string", has(Items.STRING)).save(exporter);
        RecipeBuilderShaped.shaped(Items.TURTLE_HELMET).define('X', Items.SCUTE).pattern("XXX").pattern("X X").unlockedBy("has_scute", has(Items.SCUTE)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.WHEAT, 9).requires(Blocks.HAY_BLOCK).unlockedBy("has_hay_block", has(Blocks.HAY_BLOCK)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.WHITE_DYE).requires(Items.BONE_MEAL).group("white_dye").unlockedBy("has_bone_meal", has(Items.BONE_MEAL)).save(exporter);
        oneToOneConversionRecipe(exporter, Items.WHITE_DYE, Blocks.LILY_OF_THE_VALLEY, "white_dye");
        RecipeBuilderShaped.shaped(Items.WOODEN_AXE).define('#', Items.STICK).define('X', TagsItem.PLANKS).pattern("XX").pattern("X#").pattern(" #").unlockedBy("has_stick", has(Items.STICK)).save(exporter);
        RecipeBuilderShaped.shaped(Items.WOODEN_HOE).define('#', Items.STICK).define('X', TagsItem.PLANKS).pattern("XX").pattern(" #").pattern(" #").unlockedBy("has_stick", has(Items.STICK)).save(exporter);
        RecipeBuilderShaped.shaped(Items.WOODEN_PICKAXE).define('#', Items.STICK).define('X', TagsItem.PLANKS).pattern("XXX").pattern(" # ").pattern(" # ").unlockedBy("has_stick", has(Items.STICK)).save(exporter);
        RecipeBuilderShaped.shaped(Items.WOODEN_SHOVEL).define('#', Items.STICK).define('X', TagsItem.PLANKS).pattern("X").pattern("#").pattern("#").unlockedBy("has_stick", has(Items.STICK)).save(exporter);
        RecipeBuilderShaped.shaped(Items.WOODEN_SWORD).define('#', Items.STICK).define('X', TagsItem.PLANKS).pattern("X").pattern("X").pattern("#").unlockedBy("has_stick", has(Items.STICK)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.WRITABLE_BOOK).requires(Items.BOOK).requires(Items.INK_SAC).requires(Items.FEATHER).unlockedBy("has_book", has(Items.BOOK)).save(exporter);
        oneToOneConversionRecipe(exporter, Items.YELLOW_DYE, Blocks.DANDELION, "yellow_dye");
        oneToOneConversionRecipe(exporter, Items.YELLOW_DYE, Blocks.SUNFLOWER, "yellow_dye", 2);
        nineBlockStorageRecipes(exporter, Items.DRIED_KELP, Items.DRIED_KELP_BLOCK);
        RecipeBuilderShaped.shaped(Blocks.CONDUIT).define('#', Items.NAUTILUS_SHELL).define('X', Items.HEART_OF_THE_SEA).pattern("###").pattern("#X#").pattern("###").unlockedBy("has_nautilus_core", has(Items.HEART_OF_THE_SEA)).unlockedBy("has_nautilus_shell", has(Items.NAUTILUS_SHELL)).save(exporter);
        wall(exporter, Blocks.RED_SANDSTONE_WALL, Blocks.RED_SANDSTONE);
        wall(exporter, Blocks.STONE_BRICK_WALL, Blocks.STONE_BRICKS);
        wall(exporter, Blocks.SANDSTONE_WALL, Blocks.SANDSTONE);
        RecipeBuilderShapeless.shapeless(Items.CREEPER_BANNER_PATTERN).requires(Items.PAPER).requires(Items.CREEPER_HEAD).unlockedBy("has_creeper_head", has(Items.CREEPER_HEAD)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.SKULL_BANNER_PATTERN).requires(Items.PAPER).requires(Items.WITHER_SKELETON_SKULL).unlockedBy("has_wither_skeleton_skull", has(Items.WITHER_SKELETON_SKULL)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.FLOWER_BANNER_PATTERN).requires(Items.PAPER).requires(Blocks.OXEYE_DAISY).unlockedBy("has_oxeye_daisy", has(Blocks.OXEYE_DAISY)).save(exporter);
        RecipeBuilderShapeless.shapeless(Items.MOJANG_BANNER_PATTERN).requires(Items.PAPER).requires(Items.ENCHANTED_GOLDEN_APPLE).unlockedBy("has_enchanted_golden_apple", has(Items.ENCHANTED_GOLDEN_APPLE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.SCAFFOLDING, 6).define('~', Items.STRING).define('I', Blocks.BAMBOO).pattern("I~I").pattern("I I").pattern("I I").unlockedBy("has_bamboo", has(Blocks.BAMBOO)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.GRINDSTONE).define('I', Items.STICK).define('-', Blocks.STONE_SLAB).define('#', TagsItem.PLANKS).pattern("I-I").pattern("# #").unlockedBy("has_stone_slab", has(Blocks.STONE_SLAB)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.BLAST_FURNACE).define('#', Blocks.SMOOTH_STONE).define('X', Blocks.FURNACE).define('I', Items.IRON_INGOT).pattern("III").pattern("IXI").pattern("###").unlockedBy("has_smooth_stone", has(Blocks.SMOOTH_STONE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.SMOKER).define('#', TagsItem.LOGS).define('X', Blocks.FURNACE).pattern(" # ").pattern("#X#").pattern(" # ").unlockedBy("has_furnace", has(Blocks.FURNACE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.CARTOGRAPHY_TABLE).define('#', TagsItem.PLANKS).define('@', Items.PAPER).pattern("@@").pattern("##").pattern("##").unlockedBy("has_paper", has(Items.PAPER)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.SMITHING_TABLE).define('#', TagsItem.PLANKS).define('@', Items.IRON_INGOT).pattern("@@").pattern("##").pattern("##").unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.FLETCHING_TABLE).define('#', TagsItem.PLANKS).define('@', Items.FLINT).pattern("@@").pattern("##").pattern("##").unlockedBy("has_flint", has(Items.FLINT)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.STONECUTTER).define('I', Items.IRON_INGOT).define('#', Blocks.STONE).pattern(" I ").pattern("###").unlockedBy("has_stone", has(Blocks.STONE)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.LODESTONE).define('S', Items.CHISELED_STONE_BRICKS).define('#', Items.NETHERITE_INGOT).pattern("SSS").pattern("S#S").pattern("SSS").unlockedBy("has_netherite_ingot", has(Items.NETHERITE_INGOT)).save(exporter);
        nineBlockStorageRecipesRecipesWithCustomUnpacking(exporter, Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK, "netherite_ingot_from_netherite_block", "netherite_ingot");
        RecipeBuilderShapeless.shapeless(Items.NETHERITE_INGOT).requires(Items.NETHERITE_SCRAP, 4).requires(Items.GOLD_INGOT, 4).group("netherite_ingot").unlockedBy("has_netherite_scrap", has(Items.NETHERITE_SCRAP)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.RESPAWN_ANCHOR).define('O', Blocks.CRYING_OBSIDIAN).define('G', Blocks.GLOWSTONE).pattern("OOO").pattern("GGG").pattern("OOO").unlockedBy("has_obsidian", has(Blocks.CRYING_OBSIDIAN)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.CHAIN).define('I', Items.IRON_INGOT).define('N', Items.IRON_NUGGET).pattern("N").pattern("I").pattern("N").unlockedBy("has_iron_nugget", has(Items.IRON_NUGGET)).unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.TINTED_GLASS, 2).define('G', Blocks.GLASS).define('S', Items.AMETHYST_SHARD).pattern(" S ").pattern("SGS").pattern(" S ").unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD)).save(exporter);
        RecipeBuilderShaped.shaped(Blocks.AMETHYST_BLOCK).define('S', Items.AMETHYST_SHARD).pattern("SS").pattern("SS").unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD)).save(exporter);
        RecipeBuilderSpecial.special(RecipeSerializer.ARMOR_DYE).save(exporter, "armor_dye");
        RecipeBuilderSpecial.special(RecipeSerializer.BANNER_DUPLICATE).save(exporter, "banner_duplicate");
        RecipeBuilderSpecial.special(RecipeSerializer.BOOK_CLONING).save(exporter, "book_cloning");
        RecipeBuilderSpecial.special(RecipeSerializer.FIREWORK_ROCKET).save(exporter, "firework_rocket");
        RecipeBuilderSpecial.special(RecipeSerializer.FIREWORK_STAR).save(exporter, "firework_star");
        RecipeBuilderSpecial.special(RecipeSerializer.FIREWORK_STAR_FADE).save(exporter, "firework_star_fade");
        RecipeBuilderSpecial.special(RecipeSerializer.MAP_CLONING).save(exporter, "map_cloning");
        RecipeBuilderSpecial.special(RecipeSerializer.MAP_EXTENDING).save(exporter, "map_extending");
        RecipeBuilderSpecial.special(RecipeSerializer.REPAIR_ITEM).save(exporter, "repair_item");
        RecipeBuilderSpecial.special(RecipeSerializer.SHIELD_DECORATION).save(exporter, "shield_decoration");
        RecipeBuilderSpecial.special(RecipeSerializer.SHULKER_BOX_COLORING).save(exporter, "shulker_box_coloring");
        RecipeBuilderSpecial.special(RecipeSerializer.TIPPED_ARROW).save(exporter, "tipped_arrow");
        RecipeBuilderSpecial.special(RecipeSerializer.SUSPICIOUS_STEW).save(exporter, "suspicious_stew");
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Items.POTATO), Items.BAKED_POTATO, 0.35F, 200).unlockedBy("has_potato", has(Items.POTATO)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Items.CLAY_BALL), Items.BRICK, 0.3F, 200).unlockedBy("has_clay_ball", has(Items.CLAY_BALL)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(TagsItem.LOGS_THAT_BURN), Items.CHARCOAL, 0.15F, 200).unlockedBy("has_log", has(TagsItem.LOGS_THAT_BURN)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Items.CHORUS_FRUIT), Items.POPPED_CHORUS_FRUIT, 0.1F, 200).unlockedBy("has_chorus_fruit", has(Items.CHORUS_FRUIT)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Items.BEEF), Items.COOKED_BEEF, 0.35F, 200).unlockedBy("has_beef", has(Items.BEEF)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Items.CHICKEN), Items.COOKED_CHICKEN, 0.35F, 200).unlockedBy("has_chicken", has(Items.CHICKEN)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Items.COD), Items.COOKED_COD, 0.35F, 200).unlockedBy("has_cod", has(Items.COD)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.KELP), Items.DRIED_KELP, 0.1F, 200).unlockedBy("has_kelp", has(Blocks.KELP)).save(exporter, getSmeltingRecipeName(Items.DRIED_KELP));
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Items.SALMON), Items.COOKED_SALMON, 0.35F, 200).unlockedBy("has_salmon", has(Items.SALMON)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Items.MUTTON), Items.COOKED_MUTTON, 0.35F, 200).unlockedBy("has_mutton", has(Items.MUTTON)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Items.PORKCHOP), Items.COOKED_PORKCHOP, 0.35F, 200).unlockedBy("has_porkchop", has(Items.PORKCHOP)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Items.RABBIT), Items.COOKED_RABBIT, 0.35F, 200).unlockedBy("has_rabbit", has(Items.RABBIT)).save(exporter);
        oreSmelting(exporter, COAL_SMELTABLES, Items.COAL, 0.1F, 200, "coal");
        oreSmelting(exporter, IRON_SMELTABLES, Items.IRON_INGOT, 0.7F, 200, "iron_ingot");
        oreSmelting(exporter, COPPER_SMELTABLES, Items.COPPER_INGOT, 0.7F, 200, "copper_ingot");
        oreSmelting(exporter, GOLD_SMELTABLES, Items.GOLD_INGOT, 1.0F, 200, "gold_ingot");
        oreSmelting(exporter, DIAMOND_SMELTABLES, Items.DIAMOND, 1.0F, 200, "diamond");
        oreSmelting(exporter, LAPIS_SMELTABLES, Items.LAPIS_LAZULI, 0.2F, 200, "lapis_lazuli");
        oreSmelting(exporter, REDSTONE_SMELTABLES, Items.REDSTONE, 0.7F, 200, "redstone");
        oreSmelting(exporter, EMERALD_SMELTABLES, Items.EMERALD, 1.0F, 200, "emerald");
        nineBlockStorageRecipes(exporter, Items.RAW_IRON, Items.RAW_IRON_BLOCK);
        nineBlockStorageRecipes(exporter, Items.RAW_COPPER, Items.RAW_COPPER_BLOCK);
        nineBlockStorageRecipes(exporter, Items.RAW_GOLD, Items.RAW_GOLD_BLOCK);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(TagsItem.SAND), Blocks.GLASS.getItem(), 0.1F, 200).unlockedBy("has_sand", has(TagsItem.SAND)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.SEA_PICKLE), Items.LIME_DYE, 0.1F, 200).unlockedBy("has_sea_pickle", has(Blocks.SEA_PICKLE)).save(exporter, getSmeltingRecipeName(Items.LIME_DYE));
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.CACTUS.getItem()), Items.GREEN_DYE, 1.0F, 200).unlockedBy("has_cactus", has(Blocks.CACTUS)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_AXE, Items.GOLDEN_HOE, Items.GOLDEN_SWORD, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS, Items.GOLDEN_HORSE_ARMOR), Items.GOLD_NUGGET, 0.1F, 200).unlockedBy("has_golden_pickaxe", has(Items.GOLDEN_PICKAXE)).unlockedBy("has_golden_shovel", has(Items.GOLDEN_SHOVEL)).unlockedBy("has_golden_axe", has(Items.GOLDEN_AXE)).unlockedBy("has_golden_hoe", has(Items.GOLDEN_HOE)).unlockedBy("has_golden_sword", has(Items.GOLDEN_SWORD)).unlockedBy("has_golden_helmet", has(Items.GOLDEN_HELMET)).unlockedBy("has_golden_chestplate", has(Items.GOLDEN_CHESTPLATE)).unlockedBy("has_golden_leggings", has(Items.GOLDEN_LEGGINGS)).unlockedBy("has_golden_boots", has(Items.GOLDEN_BOOTS)).unlockedBy("has_golden_horse_armor", has(Items.GOLDEN_HORSE_ARMOR)).save(exporter, getSmeltingRecipeName(Items.GOLD_NUGGET));
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_AXE, Items.IRON_HOE, Items.IRON_SWORD, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_HORSE_ARMOR, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS), Items.IRON_NUGGET, 0.1F, 200).unlockedBy("has_iron_pickaxe", has(Items.IRON_PICKAXE)).unlockedBy("has_iron_shovel", has(Items.IRON_SHOVEL)).unlockedBy("has_iron_axe", has(Items.IRON_AXE)).unlockedBy("has_iron_hoe", has(Items.IRON_HOE)).unlockedBy("has_iron_sword", has(Items.IRON_SWORD)).unlockedBy("has_iron_helmet", has(Items.IRON_HELMET)).unlockedBy("has_iron_chestplate", has(Items.IRON_CHESTPLATE)).unlockedBy("has_iron_leggings", has(Items.IRON_LEGGINGS)).unlockedBy("has_iron_boots", has(Items.IRON_BOOTS)).unlockedBy("has_iron_horse_armor", has(Items.IRON_HORSE_ARMOR)).unlockedBy("has_chainmail_helmet", has(Items.CHAINMAIL_HELMET)).unlockedBy("has_chainmail_chestplate", has(Items.CHAINMAIL_CHESTPLATE)).unlockedBy("has_chainmail_leggings", has(Items.CHAINMAIL_LEGGINGS)).unlockedBy("has_chainmail_boots", has(Items.CHAINMAIL_BOOTS)).save(exporter, getSmeltingRecipeName(Items.IRON_NUGGET));
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.CLAY), Blocks.TERRACOTTA.getItem(), 0.35F, 200).unlockedBy("has_clay_block", has(Blocks.CLAY)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.NETHERRACK), Items.NETHER_BRICK, 0.1F, 200).unlockedBy("has_netherrack", has(Blocks.NETHERRACK)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.NETHER_QUARTZ_ORE), Items.QUARTZ, 0.2F, 200).unlockedBy("has_nether_quartz_ore", has(Blocks.NETHER_QUARTZ_ORE)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.WET_SPONGE), Blocks.SPONGE.getItem(), 0.15F, 200).unlockedBy("has_wet_sponge", has(Blocks.WET_SPONGE)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.COBBLESTONE), Blocks.STONE.getItem(), 0.1F, 200).unlockedBy("has_cobblestone", has(Blocks.COBBLESTONE)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.STONE), Blocks.SMOOTH_STONE.getItem(), 0.1F, 200).unlockedBy("has_stone", has(Blocks.STONE)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.SANDSTONE), Blocks.SMOOTH_SANDSTONE.getItem(), 0.1F, 200).unlockedBy("has_sandstone", has(Blocks.SANDSTONE)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.RED_SANDSTONE), Blocks.SMOOTH_RED_SANDSTONE.getItem(), 0.1F, 200).unlockedBy("has_red_sandstone", has(Blocks.RED_SANDSTONE)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.QUARTZ_BLOCK), Blocks.SMOOTH_QUARTZ.getItem(), 0.1F, 200).unlockedBy("has_quartz_block", has(Blocks.QUARTZ_BLOCK)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.STONE_BRICKS), Blocks.CRACKED_STONE_BRICKS.getItem(), 0.1F, 200).unlockedBy("has_stone_bricks", has(Blocks.STONE_BRICKS)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.BLACK_TERRACOTTA), Blocks.BLACK_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_black_terracotta", has(Blocks.BLACK_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.BLUE_TERRACOTTA), Blocks.BLUE_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_blue_terracotta", has(Blocks.BLUE_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.BROWN_TERRACOTTA), Blocks.BROWN_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_brown_terracotta", has(Blocks.BROWN_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.CYAN_TERRACOTTA), Blocks.CYAN_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_cyan_terracotta", has(Blocks.CYAN_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.GRAY_TERRACOTTA), Blocks.GRAY_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_gray_terracotta", has(Blocks.GRAY_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.GREEN_TERRACOTTA), Blocks.GREEN_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_green_terracotta", has(Blocks.GREEN_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.LIGHT_BLUE_TERRACOTTA), Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_light_blue_terracotta", has(Blocks.LIGHT_BLUE_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.LIGHT_GRAY_TERRACOTTA), Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_light_gray_terracotta", has(Blocks.LIGHT_GRAY_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.LIME_TERRACOTTA), Blocks.LIME_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_lime_terracotta", has(Blocks.LIME_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.MAGENTA_TERRACOTTA), Blocks.MAGENTA_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_magenta_terracotta", has(Blocks.MAGENTA_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.ORANGE_TERRACOTTA), Blocks.ORANGE_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_orange_terracotta", has(Blocks.ORANGE_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.PINK_TERRACOTTA), Blocks.PINK_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_pink_terracotta", has(Blocks.PINK_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.PURPLE_TERRACOTTA), Blocks.PURPLE_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_purple_terracotta", has(Blocks.PURPLE_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.RED_TERRACOTTA), Blocks.RED_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_red_terracotta", has(Blocks.RED_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.WHITE_TERRACOTTA), Blocks.WHITE_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_white_terracotta", has(Blocks.WHITE_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.YELLOW_TERRACOTTA), Blocks.YELLOW_GLAZED_TERRACOTTA.getItem(), 0.1F, 200).unlockedBy("has_yellow_terracotta", has(Blocks.YELLOW_TERRACOTTA)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.ANCIENT_DEBRIS), Items.NETHERITE_SCRAP, 2.0F, 200).unlockedBy("has_ancient_debris", has(Blocks.ANCIENT_DEBRIS)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.BASALT), Blocks.SMOOTH_BASALT, 0.1F, 200).unlockedBy("has_basalt", has(Blocks.BASALT)).save(exporter);
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(Blocks.COBBLED_DEEPSLATE), Blocks.DEEPSLATE, 0.1F, 200).unlockedBy("has_cobbled_deepslate", has(Blocks.COBBLED_DEEPSLATE)).save(exporter);
        oreBlasting(exporter, COAL_SMELTABLES, Items.COAL, 0.1F, 100, "coal");
        oreBlasting(exporter, IRON_SMELTABLES, Items.IRON_INGOT, 0.7F, 100, "iron_ingot");
        oreBlasting(exporter, COPPER_SMELTABLES, Items.COPPER_INGOT, 0.7F, 100, "copper_ingot");
        oreBlasting(exporter, GOLD_SMELTABLES, Items.GOLD_INGOT, 1.0F, 100, "gold_ingot");
        oreBlasting(exporter, DIAMOND_SMELTABLES, Items.DIAMOND, 1.0F, 100, "diamond");
        oreBlasting(exporter, LAPIS_SMELTABLES, Items.LAPIS_LAZULI, 0.2F, 100, "lapis_lazuli");
        oreBlasting(exporter, REDSTONE_SMELTABLES, Items.REDSTONE, 0.7F, 100, "redstone");
        oreBlasting(exporter, EMERALD_SMELTABLES, Items.EMERALD, 1.0F, 100, "emerald");
        RecipeBuilderSimpleCooking.blasting(RecipeItemStack.of(Blocks.NETHER_QUARTZ_ORE), Items.QUARTZ, 0.2F, 100).unlockedBy("has_nether_quartz_ore", has(Blocks.NETHER_QUARTZ_ORE)).save(exporter, getBlastingRecipeName(Items.QUARTZ));
        RecipeBuilderSimpleCooking.blasting(RecipeItemStack.of(Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_AXE, Items.GOLDEN_HOE, Items.GOLDEN_SWORD, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS, Items.GOLDEN_HORSE_ARMOR), Items.GOLD_NUGGET, 0.1F, 100).unlockedBy("has_golden_pickaxe", has(Items.GOLDEN_PICKAXE)).unlockedBy("has_golden_shovel", has(Items.GOLDEN_SHOVEL)).unlockedBy("has_golden_axe", has(Items.GOLDEN_AXE)).unlockedBy("has_golden_hoe", has(Items.GOLDEN_HOE)).unlockedBy("has_golden_sword", has(Items.GOLDEN_SWORD)).unlockedBy("has_golden_helmet", has(Items.GOLDEN_HELMET)).unlockedBy("has_golden_chestplate", has(Items.GOLDEN_CHESTPLATE)).unlockedBy("has_golden_leggings", has(Items.GOLDEN_LEGGINGS)).unlockedBy("has_golden_boots", has(Items.GOLDEN_BOOTS)).unlockedBy("has_golden_horse_armor", has(Items.GOLDEN_HORSE_ARMOR)).save(exporter, getBlastingRecipeName(Items.GOLD_NUGGET));
        RecipeBuilderSimpleCooking.blasting(RecipeItemStack.of(Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_AXE, Items.IRON_HOE, Items.IRON_SWORD, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_HORSE_ARMOR, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS), Items.IRON_NUGGET, 0.1F, 100).unlockedBy("has_iron_pickaxe", has(Items.IRON_PICKAXE)).unlockedBy("has_iron_shovel", has(Items.IRON_SHOVEL)).unlockedBy("has_iron_axe", has(Items.IRON_AXE)).unlockedBy("has_iron_hoe", has(Items.IRON_HOE)).unlockedBy("has_iron_sword", has(Items.IRON_SWORD)).unlockedBy("has_iron_helmet", has(Items.IRON_HELMET)).unlockedBy("has_iron_chestplate", has(Items.IRON_CHESTPLATE)).unlockedBy("has_iron_leggings", has(Items.IRON_LEGGINGS)).unlockedBy("has_iron_boots", has(Items.IRON_BOOTS)).unlockedBy("has_iron_horse_armor", has(Items.IRON_HORSE_ARMOR)).unlockedBy("has_chainmail_helmet", has(Items.CHAINMAIL_HELMET)).unlockedBy("has_chainmail_chestplate", has(Items.CHAINMAIL_CHESTPLATE)).unlockedBy("has_chainmail_leggings", has(Items.CHAINMAIL_LEGGINGS)).unlockedBy("has_chainmail_boots", has(Items.CHAINMAIL_BOOTS)).save(exporter, getBlastingRecipeName(Items.IRON_NUGGET));
        RecipeBuilderSimpleCooking.blasting(RecipeItemStack.of(Blocks.ANCIENT_DEBRIS), Items.NETHERITE_SCRAP, 2.0F, 100).unlockedBy("has_ancient_debris", has(Blocks.ANCIENT_DEBRIS)).save(exporter, getBlastingRecipeName(Items.NETHERITE_SCRAP));
        cookRecipes(exporter, "smoking", RecipeSerializer.SMOKING_RECIPE, 100);
        cookRecipes(exporter, "campfire_cooking", RecipeSerializer.CAMPFIRE_COOKING_RECIPE, 600);
        stonecutterResultFromBase(exporter, Blocks.STONE_SLAB, Blocks.STONE, 2);
        stonecutterResultFromBase(exporter, Blocks.STONE_STAIRS, Blocks.STONE);
        stonecutterResultFromBase(exporter, Blocks.STONE_BRICKS, Blocks.STONE);
        stonecutterResultFromBase(exporter, Blocks.STONE_BRICK_SLAB, Blocks.STONE, 2);
        stonecutterResultFromBase(exporter, Blocks.STONE_BRICK_STAIRS, Blocks.STONE);
        RecipeBuilderSingleItem.stonecutting(RecipeItemStack.of(Blocks.STONE), Blocks.CHISELED_STONE_BRICKS).unlockedBy("has_stone", has(Blocks.STONE)).save(exporter, "chiseled_stone_bricks_stone_from_stonecutting");
        RecipeBuilderSingleItem.stonecutting(RecipeItemStack.of(Blocks.STONE), Blocks.STONE_BRICK_WALL).unlockedBy("has_stone", has(Blocks.STONE)).save(exporter, "stone_brick_walls_from_stone_stonecutting");
        stonecutterResultFromBase(exporter, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE);
        stonecutterResultFromBase(exporter, Blocks.SANDSTONE_SLAB, Blocks.SANDSTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.CUT_SANDSTONE_SLAB, Blocks.SANDSTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.CUT_SANDSTONE_SLAB, Blocks.CUT_SANDSTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.SANDSTONE_STAIRS, Blocks.SANDSTONE);
        stonecutterResultFromBase(exporter, Blocks.SANDSTONE_WALL, Blocks.SANDSTONE);
        stonecutterResultFromBase(exporter, Blocks.CHISELED_SANDSTONE, Blocks.SANDSTONE);
        stonecutterResultFromBase(exporter, Blocks.CUT_RED_SANDSTONE, Blocks.RED_SANDSTONE);
        stonecutterResultFromBase(exporter, Blocks.RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.CUT_RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.CUT_RED_SANDSTONE_SLAB, Blocks.CUT_RED_SANDSTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.RED_SANDSTONE_STAIRS, Blocks.RED_SANDSTONE);
        stonecutterResultFromBase(exporter, Blocks.RED_SANDSTONE_WALL, Blocks.RED_SANDSTONE);
        stonecutterResultFromBase(exporter, Blocks.CHISELED_RED_SANDSTONE, Blocks.RED_SANDSTONE);
        RecipeBuilderSingleItem.stonecutting(RecipeItemStack.of(Blocks.QUARTZ_BLOCK), Blocks.QUARTZ_SLAB, 2).unlockedBy("has_quartz_block", has(Blocks.QUARTZ_BLOCK)).save(exporter, "quartz_slab_from_stonecutting");
        stonecutterResultFromBase(exporter, Blocks.QUARTZ_STAIRS, Blocks.QUARTZ_BLOCK);
        stonecutterResultFromBase(exporter, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BLOCK);
        stonecutterResultFromBase(exporter, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK);
        stonecutterResultFromBase(exporter, Blocks.QUARTZ_BRICKS, Blocks.QUARTZ_BLOCK);
        stonecutterResultFromBase(exporter, Blocks.COBBLESTONE_STAIRS, Blocks.COBBLESTONE);
        stonecutterResultFromBase(exporter, Blocks.COBBLESTONE_SLAB, Blocks.COBBLESTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.COBBLESTONE_WALL, Blocks.COBBLESTONE);
        stonecutterResultFromBase(exporter, Blocks.STONE_BRICK_SLAB, Blocks.STONE_BRICKS, 2);
        stonecutterResultFromBase(exporter, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICKS);
        RecipeBuilderSingleItem.stonecutting(RecipeItemStack.of(Blocks.STONE_BRICKS), Blocks.STONE_BRICK_WALL).unlockedBy("has_stone_bricks", has(Blocks.STONE_BRICKS)).save(exporter, "stone_brick_wall_from_stone_bricks_stonecutting");
        stonecutterResultFromBase(exporter, Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS);
        stonecutterResultFromBase(exporter, Blocks.BRICK_SLAB, Blocks.BRICKS, 2);
        stonecutterResultFromBase(exporter, Blocks.BRICK_STAIRS, Blocks.BRICKS);
        stonecutterResultFromBase(exporter, Blocks.BRICK_WALL, Blocks.BRICKS);
        stonecutterResultFromBase(exporter, Blocks.NETHER_BRICK_SLAB, Blocks.NETHER_BRICKS, 2);
        stonecutterResultFromBase(exporter, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_BRICKS);
        stonecutterResultFromBase(exporter, Blocks.NETHER_BRICK_WALL, Blocks.NETHER_BRICKS);
        stonecutterResultFromBase(exporter, Blocks.CHISELED_NETHER_BRICKS, Blocks.NETHER_BRICKS);
        stonecutterResultFromBase(exporter, Blocks.RED_NETHER_BRICK_SLAB, Blocks.RED_NETHER_BRICKS, 2);
        stonecutterResultFromBase(exporter, Blocks.RED_NETHER_BRICK_STAIRS, Blocks.RED_NETHER_BRICKS);
        stonecutterResultFromBase(exporter, Blocks.RED_NETHER_BRICK_WALL, Blocks.RED_NETHER_BRICKS);
        stonecutterResultFromBase(exporter, Blocks.PURPUR_SLAB, Blocks.PURPUR_BLOCK, 2);
        stonecutterResultFromBase(exporter, Blocks.PURPUR_STAIRS, Blocks.PURPUR_BLOCK);
        stonecutterResultFromBase(exporter, Blocks.PURPUR_PILLAR, Blocks.PURPUR_BLOCK);
        stonecutterResultFromBase(exporter, Blocks.PRISMARINE_SLAB, Blocks.PRISMARINE, 2);
        stonecutterResultFromBase(exporter, Blocks.PRISMARINE_STAIRS, Blocks.PRISMARINE);
        stonecutterResultFromBase(exporter, Blocks.PRISMARINE_WALL, Blocks.PRISMARINE);
        RecipeBuilderSingleItem.stonecutting(RecipeItemStack.of(Blocks.PRISMARINE_BRICKS), Blocks.PRISMARINE_BRICK_SLAB, 2).unlockedBy("has_prismarine_brick", has(Blocks.PRISMARINE_BRICKS)).save(exporter, "prismarine_brick_slab_from_prismarine_stonecutting");
        RecipeBuilderSingleItem.stonecutting(RecipeItemStack.of(Blocks.PRISMARINE_BRICKS), Blocks.PRISMARINE_BRICK_STAIRS).unlockedBy("has_prismarine_brick", has(Blocks.PRISMARINE_BRICKS)).save(exporter, "prismarine_brick_stairs_from_prismarine_stonecutting");
        stonecutterResultFromBase(exporter, Blocks.DARK_PRISMARINE_SLAB, Blocks.DARK_PRISMARINE, 2);
        stonecutterResultFromBase(exporter, Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE);
        stonecutterResultFromBase(exporter, Blocks.ANDESITE_SLAB, Blocks.ANDESITE, 2);
        stonecutterResultFromBase(exporter, Blocks.ANDESITE_STAIRS, Blocks.ANDESITE);
        stonecutterResultFromBase(exporter, Blocks.ANDESITE_WALL, Blocks.ANDESITE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_ANDESITE, Blocks.ANDESITE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_ANDESITE_SLAB, Blocks.ANDESITE, 2);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_ANDESITE_STAIRS, Blocks.ANDESITE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_ANDESITE_SLAB, Blocks.POLISHED_ANDESITE, 2);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_ANDESITE_STAIRS, Blocks.POLISHED_ANDESITE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BASALT, Blocks.BASALT);
        stonecutterResultFromBase(exporter, Blocks.GRANITE_SLAB, Blocks.GRANITE, 2);
        stonecutterResultFromBase(exporter, Blocks.GRANITE_STAIRS, Blocks.GRANITE);
        stonecutterResultFromBase(exporter, Blocks.GRANITE_WALL, Blocks.GRANITE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_GRANITE, Blocks.GRANITE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_GRANITE_SLAB, Blocks.GRANITE, 2);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_GRANITE_STAIRS, Blocks.GRANITE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_GRANITE_SLAB, Blocks.POLISHED_GRANITE, 2);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_GRANITE_STAIRS, Blocks.POLISHED_GRANITE);
        stonecutterResultFromBase(exporter, Blocks.DIORITE_SLAB, Blocks.DIORITE, 2);
        stonecutterResultFromBase(exporter, Blocks.DIORITE_STAIRS, Blocks.DIORITE);
        stonecutterResultFromBase(exporter, Blocks.DIORITE_WALL, Blocks.DIORITE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_DIORITE, Blocks.DIORITE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_DIORITE_SLAB, Blocks.DIORITE, 2);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_DIORITE_STAIRS, Blocks.DIORITE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_DIORITE_SLAB, Blocks.POLISHED_DIORITE, 2);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_DIORITE_STAIRS, Blocks.POLISHED_DIORITE);
        RecipeBuilderSingleItem.stonecutting(RecipeItemStack.of(Blocks.MOSSY_STONE_BRICKS), Blocks.MOSSY_STONE_BRICK_SLAB, 2).unlockedBy("has_mossy_stone_bricks", has(Blocks.MOSSY_STONE_BRICKS)).save(exporter, "mossy_stone_brick_slab_from_mossy_stone_brick_stonecutting");
        RecipeBuilderSingleItem.stonecutting(RecipeItemStack.of(Blocks.MOSSY_STONE_BRICKS), Blocks.MOSSY_STONE_BRICK_STAIRS).unlockedBy("has_mossy_stone_bricks", has(Blocks.MOSSY_STONE_BRICKS)).save(exporter, "mossy_stone_brick_stairs_from_mossy_stone_brick_stonecutting");
        RecipeBuilderSingleItem.stonecutting(RecipeItemStack.of(Blocks.MOSSY_STONE_BRICKS), Blocks.MOSSY_STONE_BRICK_WALL).unlockedBy("has_mossy_stone_bricks", has(Blocks.MOSSY_STONE_BRICKS)).save(exporter, "mossy_stone_brick_wall_from_mossy_stone_brick_stonecutting");
        stonecutterResultFromBase(exporter, Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE);
        stonecutterResultFromBase(exporter, Blocks.MOSSY_COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE);
        stonecutterResultFromBase(exporter, Blocks.SMOOTH_SANDSTONE_SLAB, Blocks.SMOOTH_SANDSTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.SMOOTH_SANDSTONE_STAIRS, Blocks.SMOOTH_SANDSTONE);
        stonecutterResultFromBase(exporter, Blocks.SMOOTH_RED_SANDSTONE_SLAB, Blocks.SMOOTH_RED_SANDSTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.SMOOTH_RED_SANDSTONE_STAIRS, Blocks.SMOOTH_RED_SANDSTONE);
        stonecutterResultFromBase(exporter, Blocks.SMOOTH_QUARTZ_SLAB, Blocks.SMOOTH_QUARTZ, 2);
        stonecutterResultFromBase(exporter, Blocks.SMOOTH_QUARTZ_STAIRS, Blocks.SMOOTH_QUARTZ);
        RecipeBuilderSingleItem.stonecutting(RecipeItemStack.of(Blocks.END_STONE_BRICKS), Blocks.END_STONE_BRICK_SLAB, 2).unlockedBy("has_end_stone_brick", has(Blocks.END_STONE_BRICKS)).save(exporter, "end_stone_brick_slab_from_end_stone_brick_stonecutting");
        RecipeBuilderSingleItem.stonecutting(RecipeItemStack.of(Blocks.END_STONE_BRICKS), Blocks.END_STONE_BRICK_STAIRS).unlockedBy("has_end_stone_brick", has(Blocks.END_STONE_BRICKS)).save(exporter, "end_stone_brick_stairs_from_end_stone_brick_stonecutting");
        RecipeBuilderSingleItem.stonecutting(RecipeItemStack.of(Blocks.END_STONE_BRICKS), Blocks.END_STONE_BRICK_WALL).unlockedBy("has_end_stone_brick", has(Blocks.END_STONE_BRICKS)).save(exporter, "end_stone_brick_wall_from_end_stone_brick_stonecutting");
        stonecutterResultFromBase(exporter, Blocks.END_STONE_BRICKS, Blocks.END_STONE);
        stonecutterResultFromBase(exporter, Blocks.END_STONE_BRICK_SLAB, Blocks.END_STONE, 2);
        stonecutterResultFromBase(exporter, Blocks.END_STONE_BRICK_STAIRS, Blocks.END_STONE);
        stonecutterResultFromBase(exporter, Blocks.END_STONE_BRICK_WALL, Blocks.END_STONE);
        stonecutterResultFromBase(exporter, Blocks.SMOOTH_STONE_SLAB, Blocks.SMOOTH_STONE, 2);
        stonecutterResultFromBase(exporter, Blocks.BLACKSTONE_SLAB, Blocks.BLACKSTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.BLACKSTONE_STAIRS, Blocks.BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.BLACKSTONE_WALL, Blocks.BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE, Blocks.BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_WALL, Blocks.BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_SLAB, Blocks.BLACKSTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_STAIRS, Blocks.BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, Blocks.BLACKSTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_WALL, Blocks.BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_SLAB, Blocks.POLISHED_BLACKSTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_STAIRS, Blocks.POLISHED_BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.POLISHED_BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_WALL, Blocks.POLISHED_BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE, 2);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICKS, 2);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICKS);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_BRICKS);
        stonecutterResultFromBase(exporter, Blocks.CUT_COPPER_SLAB, Blocks.CUT_COPPER, 2);
        stonecutterResultFromBase(exporter, Blocks.CUT_COPPER_STAIRS, Blocks.CUT_COPPER);
        stonecutterResultFromBase(exporter, Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.EXPOSED_CUT_COPPER, 2);
        stonecutterResultFromBase(exporter, Blocks.EXPOSED_CUT_COPPER_STAIRS, Blocks.EXPOSED_CUT_COPPER);
        stonecutterResultFromBase(exporter, Blocks.WEATHERED_CUT_COPPER_SLAB, Blocks.WEATHERED_CUT_COPPER, 2);
        stonecutterResultFromBase(exporter, Blocks.WEATHERED_CUT_COPPER_STAIRS, Blocks.WEATHERED_CUT_COPPER);
        stonecutterResultFromBase(exporter, Blocks.OXIDIZED_CUT_COPPER_SLAB, Blocks.OXIDIZED_CUT_COPPER, 2);
        stonecutterResultFromBase(exporter, Blocks.OXIDIZED_CUT_COPPER_STAIRS, Blocks.OXIDIZED_CUT_COPPER);
        stonecutterResultFromBase(exporter, Blocks.WAXED_CUT_COPPER_SLAB, Blocks.WAXED_CUT_COPPER, 2);
        stonecutterResultFromBase(exporter, Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.WAXED_CUT_COPPER);
        stonecutterResultFromBase(exporter, Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, Blocks.WAXED_EXPOSED_CUT_COPPER, 2);
        stonecutterResultFromBase(exporter, Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS, Blocks.WAXED_EXPOSED_CUT_COPPER);
        stonecutterResultFromBase(exporter, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, Blocks.WAXED_WEATHERED_CUT_COPPER, 2);
        stonecutterResultFromBase(exporter, Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS, Blocks.WAXED_WEATHERED_CUT_COPPER);
        stonecutterResultFromBase(exporter, Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB, Blocks.WAXED_OXIDIZED_CUT_COPPER, 2);
        stonecutterResultFromBase(exporter, Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS, Blocks.WAXED_OXIDIZED_CUT_COPPER);
        stonecutterResultFromBase(exporter, Blocks.CUT_COPPER, Blocks.COPPER_BLOCK, 4);
        stonecutterResultFromBase(exporter, Blocks.CUT_COPPER_STAIRS, Blocks.COPPER_BLOCK, 4);
        stonecutterResultFromBase(exporter, Blocks.CUT_COPPER_SLAB, Blocks.COPPER_BLOCK, 8);
        stonecutterResultFromBase(exporter, Blocks.EXPOSED_CUT_COPPER, Blocks.EXPOSED_COPPER, 4);
        stonecutterResultFromBase(exporter, Blocks.EXPOSED_CUT_COPPER_STAIRS, Blocks.EXPOSED_COPPER, 4);
        stonecutterResultFromBase(exporter, Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.EXPOSED_COPPER, 8);
        stonecutterResultFromBase(exporter, Blocks.WEATHERED_CUT_COPPER, Blocks.WEATHERED_COPPER, 4);
        stonecutterResultFromBase(exporter, Blocks.WEATHERED_CUT_COPPER_STAIRS, Blocks.WEATHERED_COPPER, 4);
        stonecutterResultFromBase(exporter, Blocks.WEATHERED_CUT_COPPER_SLAB, Blocks.WEATHERED_COPPER, 8);
        stonecutterResultFromBase(exporter, Blocks.OXIDIZED_CUT_COPPER, Blocks.OXIDIZED_COPPER, 4);
        stonecutterResultFromBase(exporter, Blocks.OXIDIZED_CUT_COPPER_STAIRS, Blocks.OXIDIZED_COPPER, 4);
        stonecutterResultFromBase(exporter, Blocks.OXIDIZED_CUT_COPPER_SLAB, Blocks.OXIDIZED_COPPER, 8);
        stonecutterResultFromBase(exporter, Blocks.WAXED_CUT_COPPER, Blocks.WAXED_COPPER_BLOCK, 4);
        stonecutterResultFromBase(exporter, Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.WAXED_COPPER_BLOCK, 4);
        stonecutterResultFromBase(exporter, Blocks.WAXED_CUT_COPPER_SLAB, Blocks.WAXED_COPPER_BLOCK, 8);
        stonecutterResultFromBase(exporter, Blocks.WAXED_EXPOSED_CUT_COPPER, Blocks.WAXED_EXPOSED_COPPER, 4);
        stonecutterResultFromBase(exporter, Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS, Blocks.WAXED_EXPOSED_COPPER, 4);
        stonecutterResultFromBase(exporter, Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, Blocks.WAXED_EXPOSED_COPPER, 8);
        stonecutterResultFromBase(exporter, Blocks.WAXED_WEATHERED_CUT_COPPER, Blocks.WAXED_WEATHERED_COPPER, 4);
        stonecutterResultFromBase(exporter, Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS, Blocks.WAXED_WEATHERED_COPPER, 4);
        stonecutterResultFromBase(exporter, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, Blocks.WAXED_WEATHERED_COPPER, 8);
        stonecutterResultFromBase(exporter, Blocks.WAXED_OXIDIZED_CUT_COPPER, Blocks.WAXED_OXIDIZED_COPPER, 4);
        stonecutterResultFromBase(exporter, Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS, Blocks.WAXED_OXIDIZED_COPPER, 4);
        stonecutterResultFromBase(exporter, Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB, Blocks.WAXED_OXIDIZED_COPPER, 8);
        stonecutterResultFromBase(exporter, Blocks.COBBLED_DEEPSLATE_SLAB, Blocks.COBBLED_DEEPSLATE, 2);
        stonecutterResultFromBase(exporter, Blocks.COBBLED_DEEPSLATE_STAIRS, Blocks.COBBLED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.COBBLED_DEEPSLATE_WALL, Blocks.COBBLED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.CHISELED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.COBBLED_DEEPSLATE, 2);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_DEEPSLATE_STAIRS, Blocks.COBBLED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_DEEPSLATE_WALL, Blocks.COBBLED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_BRICKS, Blocks.COBBLED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_BRICK_SLAB, Blocks.COBBLED_DEEPSLATE, 2);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.COBBLED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_BRICK_WALL, Blocks.COBBLED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILES, Blocks.COBBLED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILE_SLAB, Blocks.COBBLED_DEEPSLATE, 2);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.COBBLED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILE_WALL, Blocks.COBBLED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.POLISHED_DEEPSLATE, 2);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_DEEPSLATE_STAIRS, Blocks.POLISHED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.POLISHED_DEEPSLATE_WALL, Blocks.POLISHED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_BRICK_SLAB, Blocks.POLISHED_DEEPSLATE, 2);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.POLISHED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_BRICK_WALL, Blocks.POLISHED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILE_SLAB, Blocks.POLISHED_DEEPSLATE, 2);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.POLISHED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILE_WALL, Blocks.POLISHED_DEEPSLATE);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_BRICK_SLAB, Blocks.DEEPSLATE_BRICKS, 2);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.DEEPSLATE_BRICKS);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_BRICK_WALL, Blocks.DEEPSLATE_BRICKS);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_BRICKS);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_BRICKS, 2);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_BRICKS);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILE_WALL, Blocks.DEEPSLATE_BRICKS);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILES, 2);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        stonecutterResultFromBase(exporter, Blocks.DEEPSLATE_TILE_WALL, Blocks.DEEPSLATE_TILES);
        netheriteSmithing(exporter, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE);
        netheriteSmithing(exporter, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS);
        netheriteSmithing(exporter, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET);
        netheriteSmithing(exporter, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS);
        netheriteSmithing(exporter, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
        netheriteSmithing(exporter, Items.DIAMOND_AXE, Items.NETHERITE_AXE);
        netheriteSmithing(exporter, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
        netheriteSmithing(exporter, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
        netheriteSmithing(exporter, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL);
    }

    private static void oneToOneConversionRecipe(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input, @Nullable String group) {
        oneToOneConversionRecipe(exporter, output, input, group, 1);
    }

    private static void oneToOneConversionRecipe(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input, @Nullable String group, int outputCount) {
        RecipeBuilderShapeless.shapeless(output, outputCount).requires(input).group(group).unlockedBy(getHasName(input), has(input)).save(exporter, getConversionRecipeName(output, input));
    }

    private static void oreSmelting(Consumer<IFinishedRecipe> exporter, List<IMaterial> inputs, IMaterial output, float experience, int cookingTime, String group) {
        oreCooking(exporter, RecipeSerializer.SMELTING_RECIPE, inputs, output, experience, cookingTime, group, "_from_smelting");
    }

    private static void oreBlasting(Consumer<IFinishedRecipe> exporter, List<IMaterial> inputs, IMaterial output, float experience, int cookingTime, String group) {
        oreCooking(exporter, RecipeSerializer.BLASTING_RECIPE, inputs, output, experience, cookingTime, group, "_from_blasting");
    }

    private static void oreCooking(Consumer<IFinishedRecipe> exporter, RecipeSerializerCooking<?> serializer, List<IMaterial> inputs, IMaterial output, float experience, int cookingTime, String group, String baseIdString) {
        for(IMaterial itemLike : inputs) {
            RecipeBuilderSimpleCooking.cooking(RecipeItemStack.of(itemLike), output, experience, cookingTime, serializer).group(group).unlockedBy(getHasName(itemLike), has(itemLike)).save(exporter, getItemName(output) + baseIdString + "_" + getItemName(itemLike));
        }

    }

    private static void netheriteSmithing(Consumer<IFinishedRecipe> exporter, Item output, Item input) {
        RecipeBuilderUpgrade.smithing(RecipeItemStack.of(output), RecipeItemStack.of(Items.NETHERITE_INGOT), input).unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT)).save(exporter, getItemName(input) + "_smithing");
    }

    private static void planksFromLog(Consumer<IFinishedRecipe> exporter, IMaterial output, Tag<Item> input) {
        RecipeBuilderShapeless.shapeless(output, 4).requires(input).group("planks").unlockedBy("has_log", has(input)).save(exporter);
    }

    private static void planksFromLogs(Consumer<IFinishedRecipe> exporter, IMaterial output, Tag<Item> input) {
        RecipeBuilderShapeless.shapeless(output, 4).requires(input).group("planks").unlockedBy("has_logs", has(input)).save(exporter);
    }

    private static void woodFromLogs(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShaped.shaped(output, 3).define('#', input).pattern("##").pattern("##").group("bark").unlockedBy("has_log", has(input)).save(exporter);
    }

    private static void woodenBoat(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShaped.shaped(output).define('#', input).pattern("# #").pattern("###").group("boat").unlockedBy("in_water", insideOf(Blocks.WATER)).save(exporter);
    }

    private static IRecipeBuilder buttonBuilder(IMaterial output, RecipeItemStack input) {
        return RecipeBuilderShapeless.shapeless(output).requires(input);
    }

    private static IRecipeBuilder doorBuilder(IMaterial output, RecipeItemStack input) {
        return RecipeBuilderShaped.shaped(output, 3).define('#', input).pattern("##").pattern("##").pattern("##");
    }

    private static IRecipeBuilder fenceBuilder(IMaterial output, RecipeItemStack input) {
        int i = output == Blocks.NETHER_BRICK_FENCE ? 6 : 3;
        Item item = output == Blocks.NETHER_BRICK_FENCE ? Items.NETHER_BRICK : Items.STICK;
        return RecipeBuilderShaped.shaped(output, i).define('W', input).define('#', item).pattern("W#W").pattern("W#W");
    }

    private static IRecipeBuilder fenceGateBuilder(IMaterial output, RecipeItemStack input) {
        return RecipeBuilderShaped.shaped(output).define('#', Items.STICK).define('W', input).pattern("#W#").pattern("#W#");
    }

    private static void pressurePlate(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        pressurePlateBuilder(output, RecipeItemStack.of(input)).unlockedBy(getHasName(input), has(input)).save(exporter);
    }

    private static IRecipeBuilder pressurePlateBuilder(IMaterial output, RecipeItemStack input) {
        return RecipeBuilderShaped.shaped(output).define('#', input).pattern("##");
    }

    private static void slab(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        slabBuilder(output, RecipeItemStack.of(input)).unlockedBy(getHasName(input), has(input)).save(exporter);
    }

    private static IRecipeBuilder slabBuilder(IMaterial output, RecipeItemStack input) {
        return RecipeBuilderShaped.shaped(output, 6).define('#', input).pattern("###");
    }

    private static IRecipeBuilder stairBuilder(IMaterial output, RecipeItemStack input) {
        return RecipeBuilderShaped.shaped(output, 4).define('#', input).pattern("#  ").pattern("## ").pattern("###");
    }

    private static IRecipeBuilder trapdoorBuilder(IMaterial output, RecipeItemStack input) {
        return RecipeBuilderShaped.shaped(output, 2).define('#', input).pattern("###").pattern("###");
    }

    private static IRecipeBuilder signBuilder(IMaterial output, RecipeItemStack input) {
        return RecipeBuilderShaped.shaped(output, 3).group("sign").define('#', input).define('X', Items.STICK).pattern("###").pattern("###").pattern(" X ");
    }

    private static void coloredWoolFromWhiteWoolAndDye(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShapeless.shapeless(output).requires(input).requires(Blocks.WHITE_WOOL).group("wool").unlockedBy("has_white_wool", has(Blocks.WHITE_WOOL)).save(exporter);
    }

    private static void carpet(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShaped.shaped(output, 3).define('#', input).pattern("##").group("carpet").unlockedBy(getHasName(input), has(input)).save(exporter);
    }

    private static void coloredCarpetFromWhiteCarpetAndDye(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShaped.shaped(output, 8).define('#', Blocks.WHITE_CARPET).define('$', input).pattern("###").pattern("#$#").pattern("###").group("carpet").unlockedBy("has_white_carpet", has(Blocks.WHITE_CARPET)).unlockedBy(getHasName(input), has(input)).save(exporter, getConversionRecipeName(output, Blocks.WHITE_CARPET));
    }

    private static void bedFromPlanksAndWool(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShaped.shaped(output).define('#', input).define('X', TagsItem.PLANKS).pattern("###").pattern("XXX").group("bed").unlockedBy(getHasName(input), has(input)).save(exporter);
    }

    private static void bedFromWhiteBedAndDye(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShapeless.shapeless(output).requires(Items.WHITE_BED).requires(input).group("dyed_bed").unlockedBy("has_bed", has(Items.WHITE_BED)).save(exporter, getConversionRecipeName(output, Items.WHITE_BED));
    }

    private static void banner(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShaped.shaped(output).define('#', input).define('|', Items.STICK).pattern("###").pattern("###").pattern(" | ").group("banner").unlockedBy(getHasName(input), has(input)).save(exporter);
    }

    private static void stainedGlassFromGlassAndDye(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShaped.shaped(output, 8).define('#', Blocks.GLASS).define('X', input).pattern("###").pattern("#X#").pattern("###").group("stained_glass").unlockedBy("has_glass", has(Blocks.GLASS)).save(exporter);
    }

    private static void stainedGlassPaneFromStainedGlass(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShaped.shaped(output, 16).define('#', input).pattern("###").pattern("###").group("stained_glass_pane").unlockedBy("has_glass", has(input)).save(exporter);
    }

    private static void stainedGlassPaneFromGlassPaneAndDye(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShaped.shaped(output, 8).define('#', Blocks.GLASS_PANE).define('$', input).pattern("###").pattern("#$#").pattern("###").group("stained_glass_pane").unlockedBy("has_glass_pane", has(Blocks.GLASS_PANE)).unlockedBy(getHasName(input), has(input)).save(exporter, getConversionRecipeName(output, Blocks.GLASS_PANE));
    }

    private static void coloredTerracottaFromTerracottaAndDye(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShaped.shaped(output, 8).define('#', Blocks.TERRACOTTA).define('X', input).pattern("###").pattern("#X#").pattern("###").group("stained_terracotta").unlockedBy("has_terracotta", has(Blocks.TERRACOTTA)).save(exporter);
    }

    private static void concretePowder(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShapeless.shapeless(output, 8).requires(input).requires(Blocks.SAND, 4).requires(Blocks.GRAVEL, 4).group("concrete_powder").unlockedBy("has_sand", has(Blocks.SAND)).unlockedBy("has_gravel", has(Blocks.GRAVEL)).save(exporter);
    }

    public static void candle(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderShapeless.shapeless(output).requires(Blocks.CANDLE).requires(input).group("dyed_candle").unlockedBy(getHasName(input), has(input)).save(exporter);
    }

    public static void wall(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        wallBuilder(output, RecipeItemStack.of(input)).unlockedBy(getHasName(input), has(input)).save(exporter);
    }

    public static IRecipeBuilder wallBuilder(IMaterial output, RecipeItemStack input) {
        return RecipeBuilderShaped.shaped(output, 6).define('#', input).pattern("###").pattern("###");
    }

    public static void polished(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        polishedBuilder(output, RecipeItemStack.of(input)).unlockedBy(getHasName(input), has(input)).save(exporter);
    }

    public static IRecipeBuilder polishedBuilder(IMaterial output, RecipeItemStack input) {
        return RecipeBuilderShaped.shaped(output, 4).define('S', input).pattern("SS").pattern("SS");
    }

    public static void cut(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        cutBuilder(output, RecipeItemStack.of(input)).unlockedBy(getHasName(input), has(input)).save(exporter);
    }

    public static RecipeBuilderShaped cutBuilder(IMaterial output, RecipeItemStack input) {
        return RecipeBuilderShaped.shaped(output, 4).define('#', input).pattern("##").pattern("##");
    }

    public static void chiseled(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        chiseledBuilder(output, RecipeItemStack.of(input)).unlockedBy(getHasName(input), has(input)).save(exporter);
    }

    public static RecipeBuilderShaped chiseledBuilder(IMaterial output, RecipeItemStack input) {
        return RecipeBuilderShaped.shaped(output).define('#', input).pattern("#").pattern("#");
    }

    private static void stonecutterResultFromBase(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        stonecutterResultFromBase(exporter, output, input, 1);
    }

    private static void stonecutterResultFromBase(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input, int count) {
        RecipeBuilderSingleItem.stonecutting(RecipeItemStack.of(input), output, count).unlockedBy(getHasName(input), has(input)).save(exporter, getConversionRecipeName(output, input) + "_stonecutting");
    }

    private static void smeltingResultFromBase(Consumer<IFinishedRecipe> exporter, IMaterial output, IMaterial input) {
        RecipeBuilderSimpleCooking.smelting(RecipeItemStack.of(input), output, 0.1F, 200).unlockedBy(getHasName(input), has(input)).save(exporter);
    }

    private static void nineBlockStorageRecipes(Consumer<IFinishedRecipe> exporter, IMaterial compacted, IMaterial input) {
        nineBlockStorageRecipes(exporter, compacted, input, getSimpleRecipeName(input), (String)null, getSimpleRecipeName(compacted), (String)null);
    }

    private static void nineBlockStorageRecipesWithCustomPacking(Consumer<IFinishedRecipe> exporter, IMaterial compacted, IMaterial input, String compactedItemId, String compactedItemGroup) {
        nineBlockStorageRecipes(exporter, compacted, input, compactedItemId, compactedItemGroup, getSimpleRecipeName(compacted), (String)null);
    }

    private static void nineBlockStorageRecipesRecipesWithCustomUnpacking(Consumer<IFinishedRecipe> exporter, IMaterial input, IMaterial compacted, String inputItemId, String inputItemGroup) {
        nineBlockStorageRecipes(exporter, input, compacted, getSimpleRecipeName(compacted), (String)null, inputItemId, inputItemGroup);
    }

    private static void nineBlockStorageRecipes(Consumer<IFinishedRecipe> exporter, IMaterial input, IMaterial compacted, String compactedItemId, @Nullable String compactedItemGroup, String inputItemId, @Nullable String inputItemGroup) {
        RecipeBuilderShapeless.shapeless(input, 9).requires(compacted).group(inputItemGroup).unlockedBy(getHasName(compacted), has(compacted)).save(exporter, new MinecraftKey(inputItemId));
        RecipeBuilderShaped.shaped(compacted).define('#', input).pattern("###").pattern("###").pattern("###").group(compactedItemGroup).unlockedBy(getHasName(input), has(input)).save(exporter, new MinecraftKey(compactedItemId));
    }

    private static void cookRecipes(Consumer<IFinishedRecipe> exporter, String cooker, RecipeSerializerCooking<?> serializer, int cookingTime) {
        simpleCookingRecipe(exporter, cooker, serializer, cookingTime, Items.BEEF, Items.COOKED_BEEF, 0.35F);
        simpleCookingRecipe(exporter, cooker, serializer, cookingTime, Items.CHICKEN, Items.COOKED_CHICKEN, 0.35F);
        simpleCookingRecipe(exporter, cooker, serializer, cookingTime, Items.COD, Items.COOKED_COD, 0.35F);
        simpleCookingRecipe(exporter, cooker, serializer, cookingTime, Items.KELP, Items.DRIED_KELP, 0.1F);
        simpleCookingRecipe(exporter, cooker, serializer, cookingTime, Items.SALMON, Items.COOKED_SALMON, 0.35F);
        simpleCookingRecipe(exporter, cooker, serializer, cookingTime, Items.MUTTON, Items.COOKED_MUTTON, 0.35F);
        simpleCookingRecipe(exporter, cooker, serializer, cookingTime, Items.PORKCHOP, Items.COOKED_PORKCHOP, 0.35F);
        simpleCookingRecipe(exporter, cooker, serializer, cookingTime, Items.POTATO, Items.BAKED_POTATO, 0.35F);
        simpleCookingRecipe(exporter, cooker, serializer, cookingTime, Items.RABBIT, Items.COOKED_RABBIT, 0.35F);
    }

    private static void simpleCookingRecipe(Consumer<IFinishedRecipe> exporter, String cooker, RecipeSerializerCooking<?> serializer, int cookingTime, IMaterial input, IMaterial output, float experience) {
        RecipeBuilderSimpleCooking.cooking(RecipeItemStack.of(input), output, experience, cookingTime, serializer).unlockedBy(getHasName(input), has(input)).save(exporter, getItemName(output) + "_from_" + cooker);
    }

    private static void waxRecipes(Consumer<IFinishedRecipe> exporter) {
        HoneycombItem.WAXABLES.get().forEach((input, output) -> {
            RecipeBuilderShapeless.shapeless(output).requires(input).requires(Items.HONEYCOMB).group(getItemName(output)).unlockedBy(getHasName(input), has(input)).save(exporter, getConversionRecipeName(output, Items.HONEYCOMB));
        });
    }

    private static void generateRecipes(Consumer<IFinishedRecipe> exporter, BlockFamily family) {
        family.getVariants().forEach((variant, block) -> {
            BiFunction<IMaterial, IMaterial, IRecipeBuilder> biFunction = shapeBuilders.get(variant);
            IMaterial itemLike = getBaseBlock(family, variant);
            if (biFunction != null) {
                IRecipeBuilder recipeBuilder = biFunction.apply(block, itemLike);
                family.getRecipeGroupPrefix().ifPresent((group) -> {
                    recipeBuilder.group(group + (variant == BlockFamily.Variant.CUT ? "" : "_" + variant.getName()));
                });
                recipeBuilder.unlockedBy(family.getRecipeUnlockedBy().orElseGet(() -> {
                    return getHasName(itemLike);
                }), has(itemLike));
                recipeBuilder.save(exporter);
            }

            if (variant == BlockFamily.Variant.CRACKED) {
                smeltingResultFromBase(exporter, block, itemLike);
            }

        });
    }

    private static Block getBaseBlock(BlockFamily family, BlockFamily.Variant variant) {
        if (variant == BlockFamily.Variant.CHISELED) {
            if (!family.getVariants().containsKey(BlockFamily.Variant.SLAB)) {
                throw new IllegalStateException("Slab is not defined for the family.");
            } else {
                return family.get(BlockFamily.Variant.SLAB);
            }
        } else {
            return family.getBaseBlock();
        }
    }

    private static CriterionTriggerEnterBlock.CriterionInstanceTrigger insideOf(Block block) {
        return new CriterionTriggerEnterBlock.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, block, CriterionTriggerProperties.ANY);
    }

    private static CriterionTriggerInventoryChanged.CriterionInstanceTrigger has(CriterionConditionValue.IntegerRange count, IMaterial item) {
        return inventoryTrigger(CriterionConditionItem.Builder.item().of(item).withCount(count).build());
    }

    private static CriterionTriggerInventoryChanged.CriterionInstanceTrigger has(IMaterial item) {
        return inventoryTrigger(CriterionConditionItem.Builder.item().of(item).build());
    }

    private static CriterionTriggerInventoryChanged.CriterionInstanceTrigger has(Tag<Item> tag) {
        return inventoryTrigger(CriterionConditionItem.Builder.item().of(tag).build());
    }

    private static CriterionTriggerInventoryChanged.CriterionInstanceTrigger inventoryTrigger(CriterionConditionItem... items) {
        return new CriterionTriggerInventoryChanged.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, CriterionConditionValue.IntegerRange.ANY, CriterionConditionValue.IntegerRange.ANY, CriterionConditionValue.IntegerRange.ANY, items);
    }

    private static String getHasName(IMaterial item) {
        return "has_" + getItemName(item);
    }

    private static String getItemName(IMaterial item) {
        return IRegistry.ITEM.getKey(item.getItem()).getKey();
    }

    private static String getSimpleRecipeName(IMaterial item) {
        return getItemName(item);
    }

    private static String getConversionRecipeName(IMaterial from, IMaterial to) {
        return getItemName(from) + "_from_" + getItemName(to);
    }

    private static String getSmeltingRecipeName(IMaterial item) {
        return getItemName(item) + "_from_smelting";
    }

    private static String getBlastingRecipeName(IMaterial item) {
        return getItemName(item) + "_from_blasting";
    }

    @Override
    public String getName() {
        return "Recipes";
    }
}
