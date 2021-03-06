package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.ChatDeserializer;

public class CriterionTriggerPlayerGeneratesContainerLoot extends CriterionTriggerAbstract<CriterionTriggerPlayerGeneratesContainerLoot.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("player_generates_container_loot");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    protected CriterionTriggerPlayerGeneratesContainerLoot.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "loot_table"));
        return new CriterionTriggerPlayerGeneratesContainerLoot.CriterionInstanceTrigger(composite, resourceLocation);
    }

    public void trigger(EntityPlayer player, MinecraftKey id) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(id);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final MinecraftKey lootTable;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite entity, MinecraftKey lootTable) {
            super(CriterionTriggerPlayerGeneratesContainerLoot.ID, entity);
            this.lootTable = lootTable;
        }

        public static CriterionTriggerPlayerGeneratesContainerLoot.CriterionInstanceTrigger lootTableUsed(MinecraftKey lootTable) {
            return new CriterionTriggerPlayerGeneratesContainerLoot.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, lootTable);
        }

        public boolean matches(MinecraftKey lootTable) {
            return this.lootTable.equals(lootTable);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.addProperty("loot_table", this.lootTable.toString());
            return jsonObject;
        }
    }
}
