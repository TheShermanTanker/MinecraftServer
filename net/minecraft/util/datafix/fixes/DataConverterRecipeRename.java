package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.schemas.Schema;
import java.util.Map;

public class DataConverterRecipeRename extends DataConverterRecipeBase {
    private static final Map<String, String> RECIPES = ImmutableMap.<String, String>builder().put("minecraft:acacia_bark", "minecraft:acacia_wood").put("minecraft:birch_bark", "minecraft:birch_wood").put("minecraft:dark_oak_bark", "minecraft:dark_oak_wood").put("minecraft:jungle_bark", "minecraft:jungle_wood").put("minecraft:oak_bark", "minecraft:oak_wood").put("minecraft:spruce_bark", "minecraft:spruce_wood").build();

    public DataConverterRecipeRename(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "Recipes renamening fix", (string) -> {
            return RECIPES.getOrDefault(string, string);
        });
    }
}
