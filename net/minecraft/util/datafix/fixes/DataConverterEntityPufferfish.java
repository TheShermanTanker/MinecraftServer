package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.schemas.Schema;
import java.util.Map;
import java.util.Objects;

public class DataConverterEntityPufferfish extends DataConverterEntityRenameAbstract {
    public static final Map<String, String> RENAMED_IDS = ImmutableMap.<String, String>builder().put("minecraft:puffer_fish_spawn_egg", "minecraft:pufferfish_spawn_egg").build();

    public DataConverterEntityPufferfish(Schema outputSchema, boolean changesType) {
        super("EntityPufferfishRenameFix", outputSchema, changesType);
    }

    @Override
    protected String rename(String oldName) {
        return Objects.equals("minecraft:puffer_fish", oldName) ? "minecraft:pufferfish" : oldName;
    }
}
