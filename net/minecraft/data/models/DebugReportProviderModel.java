package net.minecraft.data.models;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.core.IRegistry;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.data.DebugReportProvider;
import net.minecraft.data.HashCache;
import net.minecraft.data.models.blockstates.IBlockStateGenerator;
import net.minecraft.data.models.model.DelegatedModel;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DebugReportProviderModel implements DebugReportProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private final DebugReportGenerator generator;

    public DebugReportProviderModel(DebugReportGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(HashCache cache) {
        Path path = this.generator.getOutputFolder();
        Map<Block, IBlockStateGenerator> map = Maps.newHashMap();
        Consumer<IBlockStateGenerator> consumer = (blockStateGenerator) -> {
            Block block = blockStateGenerator.getBlock();
            IBlockStateGenerator blockStateGenerator2 = map.put(block, blockStateGenerator);
            if (blockStateGenerator2 != null) {
                throw new IllegalStateException("Duplicate blockstate definition for " + block);
            }
        };
        Map<MinecraftKey, Supplier<JsonElement>> map2 = Maps.newHashMap();
        Set<Item> set = Sets.newHashSet();
        BiConsumer<MinecraftKey, Supplier<JsonElement>> biConsumer = (resourceLocation, supplier) -> {
            Supplier<JsonElement> supplier2 = map2.put(resourceLocation, supplier);
            if (supplier2 != null) {
                throw new IllegalStateException("Duplicate model definition for " + resourceLocation);
            }
        };
        Consumer<Item> consumer2 = set::add;
        (new BlockModelGenerators(consumer, biConsumer, consumer2)).run();
        (new ItemModelGenerators(biConsumer)).run();
        List<Block> list = IRegistry.BLOCK.stream().filter((block) -> {
            return !map.containsKey(block);
        }).collect(Collectors.toList());
        if (!list.isEmpty()) {
            throw new IllegalStateException("Missing blockstate definitions for: " + list);
        } else {
            IRegistry.BLOCK.forEach((block) -> {
                Item item = Item.BY_BLOCK.get(block);
                if (item != null) {
                    if (set.contains(item)) {
                        return;
                    }

                    MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(item);
                    if (!map2.containsKey(resourceLocation)) {
                        map2.put(resourceLocation, new DelegatedModel(ModelLocationUtils.getModelLocation(block)));
                    }
                }

            });
            this.saveCollection(cache, path, map, DebugReportProviderModel::createBlockStatePath);
            this.saveCollection(cache, path, map2, DebugReportProviderModel::createModelPath);
        }
    }

    private <T> void saveCollection(HashCache cache, Path root, Map<T, ? extends Supplier<JsonElement>> jsons, BiFunction<Path, T, Path> locator) {
        jsons.forEach((object, supplier) -> {
            Path path2 = locator.apply(root, object);

            try {
                DebugReportProvider.save(GSON, cache, supplier.get(), path2);
            } catch (Exception var7) {
                LOGGER.error("Couldn't save {}", path2, var7);
            }

        });
    }

    private static Path createBlockStatePath(Path root, Block block) {
        MinecraftKey resourceLocation = IRegistry.BLOCK.getKey(block);
        return root.resolve("assets/" + resourceLocation.getNamespace() + "/blockstates/" + resourceLocation.getKey() + ".json");
    }

    private static Path createModelPath(Path root, MinecraftKey id) {
        return root.resolve("assets/" + id.getNamespace() + "/models/" + id.getKey() + ".json");
    }

    @Override
    public String getName() {
        return "Block State Definitions";
    }
}
