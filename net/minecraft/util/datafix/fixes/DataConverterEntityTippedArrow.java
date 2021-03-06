package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import java.util.Objects;

public class DataConverterEntityTippedArrow extends DataConverterEntityRenameAbstract {
    public DataConverterEntityTippedArrow(Schema outputSchema, boolean changesType) {
        super("EntityTippedArrowFix", outputSchema, changesType);
    }

    @Override
    protected String rename(String oldName) {
        return Objects.equals(oldName, "TippedArrow") ? "Arrow" : oldName;
    }
}
