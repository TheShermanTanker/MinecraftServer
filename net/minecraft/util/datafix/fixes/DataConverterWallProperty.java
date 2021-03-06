package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Set;

public class DataConverterWallProperty extends DataFix {
    private static final Set<String> WALL_BLOCKS = ImmutableSet.of("minecraft:andesite_wall", "minecraft:brick_wall", "minecraft:cobblestone_wall", "minecraft:diorite_wall", "minecraft:end_stone_brick_wall", "minecraft:granite_wall", "minecraft:mossy_cobblestone_wall", "minecraft:mossy_stone_brick_wall", "minecraft:nether_brick_wall", "minecraft:prismarine_wall", "minecraft:red_nether_brick_wall", "minecraft:red_sandstone_wall", "minecraft:sandstone_wall", "minecraft:stone_brick_wall");

    public DataConverterWallProperty(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("WallPropertyFix", this.getInputSchema().getType(DataConverterTypes.BLOCK_STATE), (typed) -> {
            return typed.update(DSL.remainderFinder(), DataConverterWallProperty::upgradeBlockStateTag);
        });
    }

    private static String mapProperty(String value) {
        return "true".equals(value) ? "low" : "none";
    }

    private static <T> Dynamic<T> fixWallProperty(Dynamic<T> dynamic, String string) {
        return dynamic.update(string, (dynamicx) -> {
            return DataFixUtils.orElse(dynamicx.asString().result().map(DataConverterWallProperty::mapProperty).map(dynamicx::createString), dynamicx);
        });
    }

    private static <T> Dynamic<T> upgradeBlockStateTag(Dynamic<T> dynamic) {
        boolean bl = dynamic.get("Name").asString().result().filter(WALL_BLOCKS::contains).isPresent();
        return !bl ? dynamic : dynamic.update("Properties", (dynamicx) -> {
            Dynamic<?> dynamic2 = fixWallProperty(dynamicx, "east");
            dynamic2 = fixWallProperty(dynamic2, "west");
            dynamic2 = fixWallProperty(dynamic2, "north");
            return fixWallProperty(dynamic2, "south");
        });
    }
}
