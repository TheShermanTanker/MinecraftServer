package net.minecraft.data.advancements;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementFrameType;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.CriterionConditionBlock;
import net.minecraft.advancements.critereon.CriterionConditionDamage;
import net.minecraft.advancements.critereon.CriterionConditionDamageSource;
import net.minecraft.advancements.critereon.CriterionConditionDistance;
import net.minecraft.advancements.critereon.CriterionConditionEntity;
import net.minecraft.advancements.critereon.CriterionConditionEntityEquipment;
import net.minecraft.advancements.critereon.CriterionConditionItem;
import net.minecraft.advancements.critereon.CriterionConditionLightningStrike;
import net.minecraft.advancements.critereon.CriterionConditionLocation;
import net.minecraft.advancements.critereon.CriterionConditionPlayer;
import net.minecraft.advancements.critereon.CriterionConditionValue;
import net.minecraft.advancements.critereon.CriterionTriggerChanneledLightning;
import net.minecraft.advancements.critereon.CriterionTriggerInteractBlock;
import net.minecraft.advancements.critereon.CriterionTriggerKilled;
import net.minecraft.advancements.critereon.CriterionTriggerKilledByCrossbow;
import net.minecraft.advancements.critereon.CriterionTriggerLightningStrike;
import net.minecraft.advancements.critereon.CriterionTriggerLocation;
import net.minecraft.advancements.critereon.CriterionTriggerPlayerHurtEntity;
import net.minecraft.advancements.critereon.CriterionTriggerShotCrossbow;
import net.minecraft.advancements.critereon.CriterionTriggerSlideDownBlock;
import net.minecraft.advancements.critereon.CriterionTriggerSummonedEntity;
import net.minecraft.advancements.critereon.CriterionTriggerTargetHit;
import net.minecraft.advancements.critereon.CriterionTriggerUseItem;
import net.minecraft.advancements.critereon.CriterionTriggerUsedTotem;
import net.minecraft.advancements.critereon.CriterionTriggerVillagerTrade;
import net.minecraft.advancements.critereon.DistanceTrigger;
import net.minecraft.core.IRegistry;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagsEntity;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.WorldChunkManagerMultiNoise;
import net.minecraft.world.level.block.Blocks;

public class AdvancementsAdventure implements Consumer<Consumer<Advancement>> {
    private static final int DISTANCE_FROM_BOTTOM_TO_TOP = 384;
    private static final int Y_COORDINATE_AT_TOP = 320;
    private static final int Y_COORDINATE_AT_BOTTOM = -64;
    private static final int BEDROCK_THICKNESS = 5;
    private static final EntityTypes<?>[] MOBS_TO_KILL = new EntityTypes[]{EntityTypes.BLAZE, EntityTypes.CAVE_SPIDER, EntityTypes.CREEPER, EntityTypes.DROWNED, EntityTypes.ELDER_GUARDIAN, EntityTypes.ENDER_DRAGON, EntityTypes.ENDERMAN, EntityTypes.ENDERMITE, EntityTypes.EVOKER, EntityTypes.GHAST, EntityTypes.GUARDIAN, EntityTypes.HOGLIN, EntityTypes.HUSK, EntityTypes.MAGMA_CUBE, EntityTypes.PHANTOM, EntityTypes.PIGLIN, EntityTypes.PIGLIN_BRUTE, EntityTypes.PILLAGER, EntityTypes.RAVAGER, EntityTypes.SHULKER, EntityTypes.SILVERFISH, EntityTypes.SKELETON, EntityTypes.SLIME, EntityTypes.SPIDER, EntityTypes.STRAY, EntityTypes.VEX, EntityTypes.VINDICATOR, EntityTypes.WITCH, EntityTypes.WITHER_SKELETON, EntityTypes.WITHER, EntityTypes.ZOGLIN, EntityTypes.ZOMBIE_VILLAGER, EntityTypes.ZOMBIE, EntityTypes.ZOMBIFIED_PIGLIN};

    private static CriterionTriggerLightningStrike.CriterionInstanceTrigger fireCountAndBystander(CriterionConditionValue.IntegerRange range, CriterionConditionEntity entity) {
        return CriterionTriggerLightningStrike.CriterionInstanceTrigger.lighthingStrike(CriterionConditionEntity.Builder.entity().distance(CriterionConditionDistance.absolute(CriterionConditionValue.DoubleRange.atMost(30.0D))).lighthingBolt(CriterionConditionLightningStrike.blockSetOnFire(range)).build(), entity);
    }

    private static CriterionTriggerUseItem.CriterionInstanceTrigger lookAtThroughItem(EntityTypes<?> entity, Item item) {
        return CriterionTriggerUseItem.CriterionInstanceTrigger.lookingAt(CriterionConditionEntity.Builder.entity().player(CriterionConditionPlayer.Builder.player().setLookingAt(CriterionConditionEntity.Builder.entity().of(entity).build()).build()), CriterionConditionItem.Builder.item().of(item));
    }

    @Override
    public void accept(Consumer<Advancement> consumer) {
        Advancement advancement = Advancement.SerializedAdvancement.advancement().display(Items.MAP, new ChatMessage("advancements.adventure.root.title"), new ChatMessage("advancements.adventure.root.description"), new MinecraftKey("textures/gui/advancements/backgrounds/adventure.png"), AdvancementFrameType.TASK, false, false, false).requirements(AdvancementRequirements.OR).addCriterion("killed_something", CriterionTriggerKilled.CriterionInstanceTrigger.playerKilledEntity()).addCriterion("killed_by_something", CriterionTriggerKilled.CriterionInstanceTrigger.entityKilledPlayer()).save(consumer, "adventure/root");
        Advancement advancement2 = Advancement.SerializedAdvancement.advancement().parent(advancement).display(Blocks.RED_BED, new ChatMessage("advancements.adventure.sleep_in_bed.title"), new ChatMessage("advancements.adventure.sleep_in_bed.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("slept_in_bed", CriterionTriggerLocation.CriterionInstanceTrigger.sleptInBed()).save(consumer, "adventure/sleep_in_bed");
        addBiomes(Advancement.SerializedAdvancement.advancement(), this.getAllOverworldBiomes()).parent(advancement2).display(Items.DIAMOND_BOOTS, new ChatMessage("advancements.adventure.adventuring_time.title"), new ChatMessage("advancements.adventure.adventuring_time.description"), (MinecraftKey)null, AdvancementFrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(500)).save(consumer, "adventure/adventuring_time");
        Advancement advancement3 = Advancement.SerializedAdvancement.advancement().parent(advancement).display(Items.EMERALD, new ChatMessage("advancements.adventure.trade.title"), new ChatMessage("advancements.adventure.trade.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("traded", CriterionTriggerVillagerTrade.CriterionInstanceTrigger.tradedWithVillager()).save(consumer, "adventure/trade");
        Advancement.SerializedAdvancement.advancement().parent(advancement3).display(Items.EMERALD, new ChatMessage("advancements.adventure.trade_at_world_height.title"), new ChatMessage("advancements.adventure.trade_at_world_height.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("trade_at_world_height", CriterionTriggerVillagerTrade.CriterionInstanceTrigger.tradedWithVillager(CriterionConditionEntity.Builder.entity().located(CriterionConditionLocation.atYLocation(CriterionConditionValue.DoubleRange.atLeast(319.0D))))).save(consumer, "adventure/trade_at_world_height");
        Advancement advancement4 = this.addMobsToKill(Advancement.SerializedAdvancement.advancement()).parent(advancement).display(Items.IRON_SWORD, new ChatMessage("advancements.adventure.kill_a_mob.title"), new ChatMessage("advancements.adventure.kill_a_mob.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).requirements(AdvancementRequirements.OR).save(consumer, "adventure/kill_a_mob");
        this.addMobsToKill(Advancement.SerializedAdvancement.advancement()).parent(advancement4).display(Items.DIAMOND_SWORD, new ChatMessage("advancements.adventure.kill_all_mobs.title"), new ChatMessage("advancements.adventure.kill_all_mobs.description"), (MinecraftKey)null, AdvancementFrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(100)).save(consumer, "adventure/kill_all_mobs");
        Advancement advancement5 = Advancement.SerializedAdvancement.advancement().parent(advancement4).display(Items.BOW, new ChatMessage("advancements.adventure.shoot_arrow.title"), new ChatMessage("advancements.adventure.shoot_arrow.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("shot_arrow", CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger.playerHurtEntity(CriterionConditionDamage.Builder.damageInstance().type(CriterionConditionDamageSource.Builder.damageType().isProjectile(true).direct(CriterionConditionEntity.Builder.entity().of(TagsEntity.ARROWS))))).save(consumer, "adventure/shoot_arrow");
        Advancement advancement6 = Advancement.SerializedAdvancement.advancement().parent(advancement4).display(Items.TRIDENT, new ChatMessage("advancements.adventure.throw_trident.title"), new ChatMessage("advancements.adventure.throw_trident.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("shot_trident", CriterionTriggerPlayerHurtEntity.CriterionInstanceTrigger.playerHurtEntity(CriterionConditionDamage.Builder.damageInstance().type(CriterionConditionDamageSource.Builder.damageType().isProjectile(true).direct(CriterionConditionEntity.Builder.entity().of(EntityTypes.TRIDENT))))).save(consumer, "adventure/throw_trident");
        Advancement.SerializedAdvancement.advancement().parent(advancement6).display(Items.TRIDENT, new ChatMessage("advancements.adventure.very_very_frightening.title"), new ChatMessage("advancements.adventure.very_very_frightening.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("struck_villager", CriterionTriggerChanneledLightning.CriterionInstanceTrigger.channeledLightning(CriterionConditionEntity.Builder.entity().of(EntityTypes.VILLAGER).build())).save(consumer, "adventure/very_very_frightening");
        Advancement.SerializedAdvancement.advancement().parent(advancement3).display(Blocks.CARVED_PUMPKIN, new ChatMessage("advancements.adventure.summon_iron_golem.title"), new ChatMessage("advancements.adventure.summon_iron_golem.description"), (MinecraftKey)null, AdvancementFrameType.GOAL, true, true, false).addCriterion("summoned_golem", CriterionTriggerSummonedEntity.CriterionInstanceTrigger.summonedEntity(CriterionConditionEntity.Builder.entity().of(EntityTypes.IRON_GOLEM))).save(consumer, "adventure/summon_iron_golem");
        Advancement.SerializedAdvancement.advancement().parent(advancement5).display(Items.ARROW, new ChatMessage("advancements.adventure.sniper_duel.title"), new ChatMessage("advancements.adventure.sniper_duel.description"), (MinecraftKey)null, AdvancementFrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(50)).addCriterion("killed_skeleton", CriterionTriggerKilled.CriterionInstanceTrigger.playerKilledEntity(CriterionConditionEntity.Builder.entity().of(EntityTypes.SKELETON).distance(CriterionConditionDistance.horizontal(CriterionConditionValue.DoubleRange.atLeast(50.0D))), CriterionConditionDamageSource.Builder.damageType().isProjectile(true))).save(consumer, "adventure/sniper_duel");
        Advancement.SerializedAdvancement.advancement().parent(advancement4).display(Items.TOTEM_OF_UNDYING, new ChatMessage("advancements.adventure.totem_of_undying.title"), new ChatMessage("advancements.adventure.totem_of_undying.description"), (MinecraftKey)null, AdvancementFrameType.GOAL, true, true, false).addCriterion("used_totem", CriterionTriggerUsedTotem.CriterionInstanceTrigger.usedTotem(Items.TOTEM_OF_UNDYING)).save(consumer, "adventure/totem_of_undying");
        Advancement advancement7 = Advancement.SerializedAdvancement.advancement().parent(advancement).display(Items.CROSSBOW, new ChatMessage("advancements.adventure.ol_betsy.title"), new ChatMessage("advancements.adventure.ol_betsy.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("shot_crossbow", CriterionTriggerShotCrossbow.CriterionInstanceTrigger.shotCrossbow(Items.CROSSBOW)).save(consumer, "adventure/ol_betsy");
        Advancement.SerializedAdvancement.advancement().parent(advancement7).display(Items.CROSSBOW, new ChatMessage("advancements.adventure.whos_the_pillager_now.title"), new ChatMessage("advancements.adventure.whos_the_pillager_now.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("kill_pillager", CriterionTriggerKilledByCrossbow.CriterionInstanceTrigger.crossbowKilled(CriterionConditionEntity.Builder.entity().of(EntityTypes.PILLAGER))).save(consumer, "adventure/whos_the_pillager_now");
        Advancement.SerializedAdvancement.advancement().parent(advancement7).display(Items.CROSSBOW, new ChatMessage("advancements.adventure.two_birds_one_arrow.title"), new ChatMessage("advancements.adventure.two_birds_one_arrow.description"), (MinecraftKey)null, AdvancementFrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(65)).addCriterion("two_birds", CriterionTriggerKilledByCrossbow.CriterionInstanceTrigger.crossbowKilled(CriterionConditionEntity.Builder.entity().of(EntityTypes.PHANTOM), CriterionConditionEntity.Builder.entity().of(EntityTypes.PHANTOM))).save(consumer, "adventure/two_birds_one_arrow");
        Advancement.SerializedAdvancement.advancement().parent(advancement7).display(Items.CROSSBOW, new ChatMessage("advancements.adventure.arbalistic.title"), new ChatMessage("advancements.adventure.arbalistic.description"), (MinecraftKey)null, AdvancementFrameType.CHALLENGE, true, true, true).rewards(AdvancementRewards.Builder.experience(85)).addCriterion("arbalistic", CriterionTriggerKilledByCrossbow.CriterionInstanceTrigger.crossbowKilled(CriterionConditionValue.IntegerRange.exactly(5))).save(consumer, "adventure/arbalistic");
        Advancement advancement8 = Advancement.SerializedAdvancement.advancement().parent(advancement).display(Raid.getLeaderBannerInstance(), new ChatMessage("advancements.adventure.voluntary_exile.title"), new ChatMessage("advancements.adventure.voluntary_exile.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, true).addCriterion("voluntary_exile", CriterionTriggerKilled.CriterionInstanceTrigger.playerKilledEntity(CriterionConditionEntity.Builder.entity().of(TagsEntity.RAIDERS).equipment(CriterionConditionEntityEquipment.CAPTAIN))).save(consumer, "adventure/voluntary_exile");
        Advancement.SerializedAdvancement.advancement().parent(advancement8).display(Raid.getLeaderBannerInstance(), new ChatMessage("advancements.adventure.hero_of_the_village.title"), new ChatMessage("advancements.adventure.hero_of_the_village.description"), (MinecraftKey)null, AdvancementFrameType.CHALLENGE, true, true, true).rewards(AdvancementRewards.Builder.experience(100)).addCriterion("hero_of_the_village", CriterionTriggerLocation.CriterionInstanceTrigger.raidWon()).save(consumer, "adventure/hero_of_the_village");
        Advancement.SerializedAdvancement.advancement().parent(advancement).display(Blocks.HONEY_BLOCK.getItem(), new ChatMessage("advancements.adventure.honey_block_slide.title"), new ChatMessage("advancements.adventure.honey_block_slide.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("honey_block_slide", CriterionTriggerSlideDownBlock.CriterionInstanceTrigger.slidesDownBlock(Blocks.HONEY_BLOCK)).save(consumer, "adventure/honey_block_slide");
        Advancement.SerializedAdvancement.advancement().parent(advancement5).display(Blocks.TARGET.getItem(), new ChatMessage("advancements.adventure.bullseye.title"), new ChatMessage("advancements.adventure.bullseye.description"), (MinecraftKey)null, AdvancementFrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(50)).addCriterion("bullseye", CriterionTriggerTargetHit.CriterionInstanceTrigger.targetHit(CriterionConditionValue.IntegerRange.exactly(15), CriterionConditionEntity.Composite.wrap(CriterionConditionEntity.Builder.entity().distance(CriterionConditionDistance.horizontal(CriterionConditionValue.DoubleRange.atLeast(30.0D))).build()))).save(consumer, "adventure/bullseye");
        Advancement.SerializedAdvancement.advancement().parent(advancement2).display(Items.LEATHER_BOOTS, new ChatMessage("advancements.adventure.walk_on_powder_snow_with_leather_boots.title"), new ChatMessage("advancements.adventure.walk_on_powder_snow_with_leather_boots.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("walk_on_powder_snow_with_leather_boots", CriterionTriggerLocation.CriterionInstanceTrigger.walkOnBlockWithEquipment(Blocks.POWDER_SNOW, Items.LEATHER_BOOTS)).save(consumer, "adventure/walk_on_powder_snow_with_leather_boots");
        Advancement.SerializedAdvancement.advancement().parent(advancement).display(Items.LIGHTNING_ROD, new ChatMessage("advancements.adventure.lightning_rod_with_villager_no_fire.title"), new ChatMessage("advancements.adventure.lightning_rod_with_villager_no_fire.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("lightning_rod_with_villager_no_fire", fireCountAndBystander(CriterionConditionValue.IntegerRange.exactly(0), CriterionConditionEntity.Builder.entity().of(EntityTypes.VILLAGER).build())).save(consumer, "adventure/lightning_rod_with_villager_no_fire");
        Advancement advancement9 = Advancement.SerializedAdvancement.advancement().parent(advancement).display(Items.SPYGLASS, new ChatMessage("advancements.adventure.spyglass_at_parrot.title"), new ChatMessage("advancements.adventure.spyglass_at_parrot.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("spyglass_at_parrot", lookAtThroughItem(EntityTypes.PARROT, Items.SPYGLASS)).save(consumer, "adventure/spyglass_at_parrot");
        Advancement advancement10 = Advancement.SerializedAdvancement.advancement().parent(advancement9).display(Items.SPYGLASS, new ChatMessage("advancements.adventure.spyglass_at_ghast.title"), new ChatMessage("advancements.adventure.spyglass_at_ghast.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("spyglass_at_ghast", lookAtThroughItem(EntityTypes.GHAST, Items.SPYGLASS)).save(consumer, "adventure/spyglass_at_ghast");
        Advancement.SerializedAdvancement.advancement().parent(advancement2).display(Items.JUKEBOX, new ChatMessage("advancements.adventure.play_jukebox_in_meadows.title"), new ChatMessage("advancements.adventure.play_jukebox_in_meadows.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("play_jukebox_in_meadows", CriterionTriggerInteractBlock.CriterionInstanceTrigger.itemUsedOnBlock(CriterionConditionLocation.Builder.location().setBiome(Biomes.MEADOW).setBlock(CriterionConditionBlock.Builder.block().of(Blocks.JUKEBOX).build()), CriterionConditionItem.Builder.item().of(TagsItem.MUSIC_DISCS))).save(consumer, "adventure/play_jukebox_in_meadows");
        Advancement.SerializedAdvancement.advancement().parent(advancement10).display(Items.SPYGLASS, new ChatMessage("advancements.adventure.spyglass_at_dragon.title"), new ChatMessage("advancements.adventure.spyglass_at_dragon.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("spyglass_at_dragon", lookAtThroughItem(EntityTypes.ENDER_DRAGON, Items.SPYGLASS)).save(consumer, "adventure/spyglass_at_dragon");
        Advancement.SerializedAdvancement.advancement().parent(advancement).display(Items.WATER_BUCKET, new ChatMessage("advancements.adventure.fall_from_world_height.title"), new ChatMessage("advancements.adventure.fall_from_world_height.description"), (MinecraftKey)null, AdvancementFrameType.TASK, true, true, false).addCriterion("fall_from_world_height", DistanceTrigger.TriggerInstance.fallFromHeight(CriterionConditionEntity.Builder.entity().located(CriterionConditionLocation.atYLocation(CriterionConditionValue.DoubleRange.atMost(-59.0D))), CriterionConditionDistance.vertical(CriterionConditionValue.DoubleRange.atLeast(379.0D)), CriterionConditionLocation.atYLocation(CriterionConditionValue.DoubleRange.atLeast(319.0D)))).save(consumer, "adventure/fall_from_world_height");
    }

    private List<ResourceKey<BiomeBase>> getAllOverworldBiomes() {
        Set<BiomeBase> set = WorldChunkManagerMultiNoise.Preset.OVERWORLD.biomeSource(RegistryGeneration.BIOME).possibleBiomes();
        return set.stream().map(RegistryGeneration.BIOME::getResourceKey).flatMap(Optional::stream).collect(Collectors.toList());
    }

    private Advancement.SerializedAdvancement addMobsToKill(Advancement.SerializedAdvancement task) {
        for(EntityTypes<?> entityType : MOBS_TO_KILL) {
            task.addCriterion(IRegistry.ENTITY_TYPE.getKey(entityType).toString(), CriterionTriggerKilled.CriterionInstanceTrigger.playerKilledEntity(CriterionConditionEntity.Builder.entity().of(entityType)));
        }

        return task;
    }

    protected static Advancement.SerializedAdvancement addBiomes(Advancement.SerializedAdvancement task, List<ResourceKey<BiomeBase>> biomes) {
        for(ResourceKey<BiomeBase> resourceKey : biomes) {
            task.addCriterion(resourceKey.location().toString(), CriterionTriggerLocation.CriterionInstanceTrigger.located(CriterionConditionLocation.inBiome(resourceKey)));
        }

        return task;
    }
}
