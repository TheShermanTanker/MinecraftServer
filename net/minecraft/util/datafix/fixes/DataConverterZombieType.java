package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;

public class DataConverterZombieType extends DataConverterEntityNameAbstract {
    public DataConverterZombieType(Schema outputSchema, boolean changesType) {
        super("EntityZombieSplitFix", outputSchema, changesType);
    }

    @Override
    protected Pair<String, Dynamic<?>> getNewNameAndTag(String choice, Dynamic<?> dynamic) {
        if (Objects.equals("Zombie", choice)) {
            String string = "Zombie";
            int i = dynamic.get("ZombieType").asInt(0);
            switch(i) {
            case 0:
            default:
                break;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                string = "ZombieVillager";
                dynamic = dynamic.set("Profession", dynamic.createInt(i - 1));
                break;
            case 6:
                string = "Husk";
            }

            dynamic = dynamic.remove("ZombieType");
            return Pair.of(string, dynamic);
        } else {
            return Pair.of(choice, dynamic);
        }
    }
}
