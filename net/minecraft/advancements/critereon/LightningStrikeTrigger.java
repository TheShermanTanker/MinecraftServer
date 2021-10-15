package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class LightningStrikeTrigger extends CriterionTriggerAbstract<LightningStrikeTrigger.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("lightning_strike");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public LightningStrikeTrigger.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionEntity.Composite composite2 = CriterionConditionEntity.Composite.fromJson(jsonObject, "lightning", deserializationContext);
        CriterionConditionEntity.Composite composite3 = CriterionConditionEntity.Composite.fromJson(jsonObject, "bystander", deserializationContext);
        return new LightningStrikeTrigger.TriggerInstance(composite, composite2, composite3);
    }

    public void trigger(EntityPlayer player, EntityLightning lightning, List<Entity> bystanders) {
        List<LootTableInfo> list = bystanders.stream().map((bystander) -> {
            return CriterionConditionEntity.createContext(player, bystander);
        }).collect(Collectors.toList());
        LootTableInfo lootContext = CriterionConditionEntity.createContext(player, lightning);
        this.trigger(player, (conditions) -> {
            return conditions.matches(lootContext, list);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionEntity.Composite lightning;
        private final CriterionConditionEntity.Composite bystander;

        public TriggerInstance(CriterionConditionEntity.Composite player, CriterionConditionEntity.Composite lightning, CriterionConditionEntity.Composite bystander) {
            super(LightningStrikeTrigger.ID, player);
            this.lightning = lightning;
            this.bystander = bystander;
        }

        public static LightningStrikeTrigger.TriggerInstance lighthingStrike(CriterionConditionEntity lightning, CriterionConditionEntity bystander) {
            return new LightningStrikeTrigger.TriggerInstance(CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.wrap(lightning), CriterionConditionEntity.Composite.wrap(bystander));
        }

        public boolean matches(LootTableInfo lightning, List<LootTableInfo> bystanders) {
            if (!this.lightning.matches(lightning)) {
                return false;
            } else {
                return this.bystander == CriterionConditionEntity.Composite.ANY || !bystanders.stream().noneMatch(this.bystander::matches);
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("lightning", this.lightning.toJson(predicateSerializer));
            jsonObject.add("bystander", this.bystander.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
