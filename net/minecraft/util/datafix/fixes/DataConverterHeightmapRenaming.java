package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class DataConverterHeightmapRenaming extends DataFix {
    public DataConverterHeightmapRenaming(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(DataConverterTypes.CHUNK);
        OpticFinder<?> opticFinder = type.findField("Level");
        return this.fixTypeEverywhereTyped("HeightmapRenamingFix", type, (typed) -> {
            return typed.updateTyped(opticFinder, (typedx) -> {
                return typedx.update(DSL.remainderFinder(), this::fix);
            });
        });
    }

    private Dynamic<?> fix(Dynamic<?> dynamic) {
        Optional<? extends Dynamic<?>> optional = dynamic.get("Heightmaps").result();
        if (!optional.isPresent()) {
            return dynamic;
        } else {
            Dynamic<?> dynamic2 = optional.get();
            Optional<? extends Dynamic<?>> optional2 = dynamic2.get("LIQUID").result();
            if (optional2.isPresent()) {
                dynamic2 = dynamic2.remove("LIQUID");
                dynamic2 = dynamic2.set("WORLD_SURFACE_WG", optional2.get());
            }

            Optional<? extends Dynamic<?>> optional3 = dynamic2.get("SOLID").result();
            if (optional3.isPresent()) {
                dynamic2 = dynamic2.remove("SOLID");
                dynamic2 = dynamic2.set("OCEAN_FLOOR_WG", optional3.get());
                dynamic2 = dynamic2.set("OCEAN_FLOOR", optional3.get());
            }

            Optional<? extends Dynamic<?>> optional4 = dynamic2.get("LIGHT").result();
            if (optional4.isPresent()) {
                dynamic2 = dynamic2.remove("LIGHT");
                dynamic2 = dynamic2.set("LIGHT_BLOCKING", optional4.get());
            }

            Optional<? extends Dynamic<?>> optional5 = dynamic2.get("RAIN").result();
            if (optional5.isPresent()) {
                dynamic2 = dynamic2.remove("RAIN");
                dynamic2 = dynamic2.set("MOTION_BLOCKING", optional5.get());
                dynamic2 = dynamic2.set("MOTION_BLOCKING_NO_LEAVES", optional5.get());
            }

            return dynamic.set("Heightmaps", dynamic2);
        }
    }
}
