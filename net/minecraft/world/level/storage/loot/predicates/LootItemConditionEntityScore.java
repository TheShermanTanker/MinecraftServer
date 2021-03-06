package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardObjective;

public class LootItemConditionEntityScore implements LootItemCondition {
    final Map<String, IntRange> scores;
    final LootTableInfo.EntityTarget entityTarget;

    LootItemConditionEntityScore(Map<String, IntRange> scores, LootTableInfo.EntityTarget target) {
        this.scores = ImmutableMap.copyOf(scores);
        this.entityTarget = target;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.ENTITY_SCORES;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return Stream.concat(Stream.of(this.entityTarget.getParam()), this.scores.values().stream().flatMap((intRange) -> {
            return intRange.getReferencedContextParams().stream();
        })).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        Entity entity = lootContext.getContextParameter(this.entityTarget.getParam());
        if (entity == null) {
            return false;
        } else {
            Scoreboard scoreboard = entity.level.getScoreboard();

            for(Entry<String, IntRange> entry : this.scores.entrySet()) {
                if (!this.hasScore(lootContext, entity, scoreboard, entry.getKey(), entry.getValue())) {
                    return false;
                }
            }

            return true;
        }
    }

    protected boolean hasScore(LootTableInfo context, Entity entity, Scoreboard scoreboard, String objectiveName, IntRange range) {
        ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            return false;
        } else {
            String string = entity.getName();
            return !scoreboard.hasPlayerScore(string, objective) ? false : range.test(context, scoreboard.getPlayerScoreForObjective(string, objective).getScore());
        }
    }

    public static LootItemConditionEntityScore.Builder hasScores(LootTableInfo.EntityTarget target) {
        return new LootItemConditionEntityScore.Builder(target);
    }

    public static class Builder implements LootItemCondition.Builder {
        private final Map<String, IntRange> scores = Maps.newHashMap();
        private final LootTableInfo.EntityTarget entityTarget;

        public Builder(LootTableInfo.EntityTarget target) {
            this.entityTarget = target;
        }

        public LootItemConditionEntityScore.Builder withScore(String name, IntRange value) {
            this.scores.put(name, value);
            return this;
        }

        @Override
        public LootItemCondition build() {
            return new LootItemConditionEntityScore(this.scores, this.entityTarget);
        }
    }

    public static class Serializer implements LootSerializer<LootItemConditionEntityScore> {
        @Override
        public void serialize(JsonObject json, LootItemConditionEntityScore object, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();

            for(Entry<String, IntRange> entry : object.scores.entrySet()) {
                jsonObject.add(entry.getKey(), context.serialize(entry.getValue()));
            }

            json.add("scores", jsonObject);
            json.add("entity", context.serialize(object.entityTarget));
        }

        @Override
        public LootItemConditionEntityScore deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            Set<Entry<String, JsonElement>> set = ChatDeserializer.getAsJsonObject(jsonObject, "scores").entrySet();
            Map<String, IntRange> map = Maps.newLinkedHashMap();

            for(Entry<String, JsonElement> entry : set) {
                map.put(entry.getKey(), ChatDeserializer.convertToObject(entry.getValue(), "score", jsonDeserializationContext, IntRange.class));
            }

            return new LootItemConditionEntityScore(map, ChatDeserializer.getAsObject(jsonObject, "entity", jsonDeserializationContext, LootTableInfo.EntityTarget.class));
        }
    }
}
