package net.minecraft.data.models.blockstates;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.minecraft.SystemUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class BlockStateGeneratorMultiVariant implements IBlockStateGenerator {
    private final Block block;
    private final List<Variant> baseVariants;
    private final Set<IBlockState<?>> seenProperties = Sets.newHashSet();
    private final List<PropertyDispatch> declaredPropertySets = Lists.newArrayList();

    private BlockStateGeneratorMultiVariant(Block block, List<Variant> variants) {
        this.block = block;
        this.baseVariants = variants;
    }

    public BlockStateGeneratorMultiVariant with(PropertyDispatch map) {
        map.getDefinedProperties().forEach((property) -> {
            if (this.block.getStates().getProperty(property.getName()) != property) {
                throw new IllegalStateException("Property " + property + " is not defined for block " + this.block);
            } else if (!this.seenProperties.add(property)) {
                throw new IllegalStateException("Values of property " + property + " already defined for block " + this.block);
            }
        });
        this.declaredPropertySets.add(map);
        return this;
    }

    @Override
    public JsonElement get() {
        Stream<Pair<Selector, List<Variant>>> stream = Stream.of(Pair.of(Selector.empty(), this.baseVariants));

        for(PropertyDispatch propertyDispatch : this.declaredPropertySets) {
            Map<Selector, List<Variant>> map = propertyDispatch.getEntries();
            stream = stream.flatMap((pair) -> {
                return map.entrySet().stream().map((entry) -> {
                    Selector selector = ((Selector)pair.getFirst()).extend(entry.getKey());
                    List<Variant> list = mergeVariants((List)pair.getSecond(), entry.getValue());
                    return Pair.of(selector, list);
                });
            });
        }

        Map<String, JsonElement> map2 = new TreeMap<>();
        stream.forEach((pair) -> {
            map2.put(pair.getFirst().getKey(), Variant.convertList(pair.getSecond()));
        });
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("variants", SystemUtils.make(new JsonObject(), (jsonObjectx) -> {
            map2.forEach(jsonObjectx::add);
        }));
        return jsonObject;
    }

    private static List<Variant> mergeVariants(List<Variant> left, List<Variant> right) {
        Builder<Variant> builder = ImmutableList.builder();
        left.forEach((variant) -> {
            right.forEach((variant2) -> {
                builder.add(Variant.merge(variant, variant2));
            });
        });
        return builder.build();
    }

    @Override
    public Block getBlock() {
        return this.block;
    }

    public static BlockStateGeneratorMultiVariant multiVariant(Block block) {
        return new BlockStateGeneratorMultiVariant(block, ImmutableList.of(Variant.variant()));
    }

    public static BlockStateGeneratorMultiVariant multiVariant(Block block, Variant variant) {
        return new BlockStateGeneratorMultiVariant(block, ImmutableList.of(variant));
    }

    public static BlockStateGeneratorMultiVariant multiVariant(Block block, Variant... variants) {
        return new BlockStateGeneratorMultiVariant(block, ImmutableList.copyOf(variants));
    }
}
