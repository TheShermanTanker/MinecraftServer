package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.schemas.Schema;
import java.util.Map;
import java.util.Objects;

public class DataConverterEntityRavagerRename extends DataConverterEntityRenameAbstract {
    public static final Map<String, String> RENAMED_IDS = ImmutableMap.<String, String>builder().put("minecraft:illager_beast_spawn_egg", "minecraft:ravager_spawn_egg").build();

    public DataConverterEntityRavagerRename(Schema outputSchema, boolean changesType) {
        super("EntityRavagerRenameFix", outputSchema, changesType);
    }

    @Override
    protected String rename(String oldName) {
        return Objects.equals("minecraft:illager_beast", oldName) ? "minecraft:ravager" : oldName;
    }
}
