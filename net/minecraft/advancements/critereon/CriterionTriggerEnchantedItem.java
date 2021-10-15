package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.item.ItemStack;

public class CriterionTriggerEnchantedItem extends CriterionTriggerAbstract<CriterionTriggerEnchantedItem.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("enchanted_item");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerEnchantedItem.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("item"));
        CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("levels"));
        return new CriterionTriggerEnchantedItem.TriggerInstance(composite, itemPredicate, ints);
    }

    public void trigger(EntityPlayer player, ItemStack stack, int levels) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(stack, levels);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionItem item;
        private final CriterionConditionValue.IntegerRange levels;

        public TriggerInstance(CriterionConditionEntity.Composite player, CriterionConditionItem item, CriterionConditionValue.IntegerRange levels) {
            super(CriterionTriggerEnchantedItem.ID, player);
            this.item = item;
            this.levels = levels;
        }

        public static CriterionTriggerEnchantedItem.TriggerInstance enchantedItem() {
            return new CriterionTriggerEnchantedItem.TriggerInstance(CriterionConditionEntity.Composite.ANY, CriterionConditionItem.ANY, CriterionConditionValue.IntegerRange.ANY);
        }

        public boolean matches(ItemStack stack, int levels) {
            if (!this.item.matches(stack)) {
                return false;
            } else {
                return this.levels.matches(levels);
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("item", this.item.serializeToJson());
            jsonObject.add("levels", this.levels.serializeToJson());
            return jsonObject;
        }
    }
}
