package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class CriterionTriggerLocation extends CriterionTriggerAbstract<CriterionTriggerLocation.TriggerInstance> {
    final MinecraftKey id;

    public CriterionTriggerLocation(MinecraftKey id) {
        this.id = id;
    }

    @Override
    public MinecraftKey getId() {
        return this.id;
    }

    @Override
    public CriterionTriggerLocation.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        JsonObject jsonObject2 = ChatDeserializer.getAsJsonObject(jsonObject, "location", jsonObject);
        CriterionConditionLocation locationPredicate = CriterionConditionLocation.fromJson(jsonObject2);
        return new CriterionTriggerLocation.TriggerInstance(this.id, composite, locationPredicate);
    }

    public void trigger(EntityPlayer player) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(player.getWorldServer(), player.locX(), player.locY(), player.locZ());
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionLocation location;

        public TriggerInstance(MinecraftKey id, CriterionConditionEntity.Composite player, CriterionConditionLocation location) {
            super(id, player);
            this.location = location;
        }

        public static CriterionTriggerLocation.TriggerInstance located(CriterionConditionLocation location) {
            return new CriterionTriggerLocation.TriggerInstance(CriterionTriggers.LOCATION.id, CriterionConditionEntity.Composite.ANY, location);
        }

        public static CriterionTriggerLocation.TriggerInstance located(CriterionConditionEntity entity) {
            return new CriterionTriggerLocation.TriggerInstance(CriterionTriggers.LOCATION.id, CriterionConditionEntity.Composite.wrap(entity), CriterionConditionLocation.ANY);
        }

        public static CriterionTriggerLocation.TriggerInstance sleptInBed() {
            return new CriterionTriggerLocation.TriggerInstance(CriterionTriggers.SLEPT_IN_BED.id, CriterionConditionEntity.Composite.ANY, CriterionConditionLocation.ANY);
        }

        public static CriterionTriggerLocation.TriggerInstance raidWon() {
            return new CriterionTriggerLocation.TriggerInstance(CriterionTriggers.RAID_WIN.id, CriterionConditionEntity.Composite.ANY, CriterionConditionLocation.ANY);
        }

        public static CriterionTriggerLocation.TriggerInstance walkOnBlockWithEquipment(Block block, Item boots) {
            return located(CriterionConditionEntity.Builder.entity().equipment(CriterionConditionEntityEquipment.Builder.equipment().feet(CriterionConditionItem.Builder.item().of(boots).build()).build()).steppingOn(CriterionConditionLocation.Builder.location().setBlock(CriterionConditionBlock.Builder.block().of(block).build()).build()).build());
        }

        public boolean matches(WorldServer world, double x, double y, double z) {
            return this.location.matches(world, x, y, z);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("location", this.location.serializeToJson());
            return jsonObject;
        }
    }
}
