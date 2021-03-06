package net.minecraft.util.datafix.schemas;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.DataConverterTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataConverterSchemaV99 extends Schema {
    private static final Logger LOGGER = LogManager.getLogger();
    static final Map<String, String> ITEM_TO_BLOCKENTITY = DataFixUtils.make(Maps.newHashMap(), (hashMap) -> {
        hashMap.put("minecraft:furnace", "Furnace");
        hashMap.put("minecraft:lit_furnace", "Furnace");
        hashMap.put("minecraft:chest", "Chest");
        hashMap.put("minecraft:trapped_chest", "Chest");
        hashMap.put("minecraft:ender_chest", "EnderChest");
        hashMap.put("minecraft:jukebox", "RecordPlayer");
        hashMap.put("minecraft:dispenser", "Trap");
        hashMap.put("minecraft:dropper", "Dropper");
        hashMap.put("minecraft:sign", "Sign");
        hashMap.put("minecraft:mob_spawner", "MobSpawner");
        hashMap.put("minecraft:noteblock", "Music");
        hashMap.put("minecraft:brewing_stand", "Cauldron");
        hashMap.put("minecraft:enhanting_table", "EnchantTable");
        hashMap.put("minecraft:command_block", "CommandBlock");
        hashMap.put("minecraft:beacon", "Beacon");
        hashMap.put("minecraft:skull", "Skull");
        hashMap.put("minecraft:daylight_detector", "DLDetector");
        hashMap.put("minecraft:hopper", "Hopper");
        hashMap.put("minecraft:banner", "Banner");
        hashMap.put("minecraft:flower_pot", "FlowerPot");
        hashMap.put("minecraft:repeating_command_block", "CommandBlock");
        hashMap.put("minecraft:chain_command_block", "CommandBlock");
        hashMap.put("minecraft:standing_sign", "Sign");
        hashMap.put("minecraft:wall_sign", "Sign");
        hashMap.put("minecraft:piston_head", "Piston");
        hashMap.put("minecraft:daylight_detector_inverted", "DLDetector");
        hashMap.put("minecraft:unpowered_comparator", "Comparator");
        hashMap.put("minecraft:powered_comparator", "Comparator");
        hashMap.put("minecraft:wall_banner", "Banner");
        hashMap.put("minecraft:standing_banner", "Banner");
        hashMap.put("minecraft:structure_block", "Structure");
        hashMap.put("minecraft:end_portal", "Airportal");
        hashMap.put("minecraft:end_gateway", "EndGateway");
        hashMap.put("minecraft:shield", "Banner");
    });
    protected static final HookFunction ADD_NAMES = new HookFunction() {
        public <T> T apply(DynamicOps<T> dynamicOps, T object) {
            return DataConverterSchemaV99.addNames(new Dynamic<>(dynamicOps, object), DataConverterSchemaV99.ITEM_TO_BLOCKENTITY, "ArmorStand");
        }
    };

    public DataConverterSchemaV99(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    protected static TypeTemplate equipment(Schema schema) {
        return DSL.optionalFields("Equipment", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)));
    }

    protected static void registerMob(Schema schema, Map<String, Supplier<TypeTemplate>> map, String entityId) {
        schema.register(map, entityId, () -> {
            return equipment(schema);
        });
    }

    protected static void registerThrowableProjectile(Schema schema, Map<String, Supplier<TypeTemplate>> map, String entityId) {
        schema.register(map, entityId, () -> {
            return DSL.optionalFields("inTile", DataConverterTypes.BLOCK_NAME.in(schema));
        });
    }

    protected static void registerMinecart(Schema schema, Map<String, Supplier<TypeTemplate>> map, String entityId) {
        schema.register(map, entityId, () -> {
            return DSL.optionalFields("DisplayTile", DataConverterTypes.BLOCK_NAME.in(schema));
        });
    }

    protected static void registerInventory(Schema schema, Map<String, Supplier<TypeTemplate>> map, String entityId) {
        schema.register(map, entityId, () -> {
            return DSL.optionalFields("Items", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)));
        });
    }

    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
        schema.register(map, "Item", (string) -> {
            return DSL.optionalFields("Item", DataConverterTypes.ITEM_STACK.in(schema));
        });
        schema.registerSimple(map, "XPOrb");
        registerThrowableProjectile(schema, map, "ThrownEgg");
        schema.registerSimple(map, "LeashKnot");
        schema.registerSimple(map, "Painting");
        schema.register(map, "Arrow", (string) -> {
            return DSL.optionalFields("inTile", DataConverterTypes.BLOCK_NAME.in(schema));
        });
        schema.register(map, "TippedArrow", (string) -> {
            return DSL.optionalFields("inTile", DataConverterTypes.BLOCK_NAME.in(schema));
        });
        schema.register(map, "SpectralArrow", (string) -> {
            return DSL.optionalFields("inTile", DataConverterTypes.BLOCK_NAME.in(schema));
        });
        registerThrowableProjectile(schema, map, "Snowball");
        registerThrowableProjectile(schema, map, "Fireball");
        registerThrowableProjectile(schema, map, "SmallFireball");
        registerThrowableProjectile(schema, map, "ThrownEnderpearl");
        schema.registerSimple(map, "EyeOfEnderSignal");
        schema.register(map, "ThrownPotion", (string) -> {
            return DSL.optionalFields("inTile", DataConverterTypes.BLOCK_NAME.in(schema), "Potion", DataConverterTypes.ITEM_STACK.in(schema));
        });
        registerThrowableProjectile(schema, map, "ThrownExpBottle");
        schema.register(map, "ItemFrame", (string) -> {
            return DSL.optionalFields("Item", DataConverterTypes.ITEM_STACK.in(schema));
        });
        registerThrowableProjectile(schema, map, "WitherSkull");
        schema.registerSimple(map, "PrimedTnt");
        schema.register(map, "FallingSand", (string) -> {
            return DSL.optionalFields("Block", DataConverterTypes.BLOCK_NAME.in(schema), "TileEntityData", DataConverterTypes.BLOCK_ENTITY.in(schema));
        });
        schema.register(map, "FireworksRocketEntity", (string) -> {
            return DSL.optionalFields("FireworksItem", DataConverterTypes.ITEM_STACK.in(schema));
        });
        schema.registerSimple(map, "Boat");
        schema.register(map, "Minecart", () -> {
            return DSL.optionalFields("DisplayTile", DataConverterTypes.BLOCK_NAME.in(schema), "Items", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)));
        });
        registerMinecart(schema, map, "MinecartRideable");
        schema.register(map, "MinecartChest", (string) -> {
            return DSL.optionalFields("DisplayTile", DataConverterTypes.BLOCK_NAME.in(schema), "Items", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)));
        });
        registerMinecart(schema, map, "MinecartFurnace");
        registerMinecart(schema, map, "MinecartTNT");
        schema.register(map, "MinecartSpawner", () -> {
            return DSL.optionalFields("DisplayTile", DataConverterTypes.BLOCK_NAME.in(schema), DataConverterTypes.UNTAGGED_SPAWNER.in(schema));
        });
        schema.register(map, "MinecartHopper", (string) -> {
            return DSL.optionalFields("DisplayTile", DataConverterTypes.BLOCK_NAME.in(schema), "Items", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)));
        });
        registerMinecart(schema, map, "MinecartCommandBlock");
        registerMob(schema, map, "ArmorStand");
        registerMob(schema, map, "Creeper");
        registerMob(schema, map, "Skeleton");
        registerMob(schema, map, "Spider");
        registerMob(schema, map, "Giant");
        registerMob(schema, map, "Zombie");
        registerMob(schema, map, "Slime");
        registerMob(schema, map, "Ghast");
        registerMob(schema, map, "PigZombie");
        schema.register(map, "Enderman", (string) -> {
            return DSL.optionalFields("carried", DataConverterTypes.BLOCK_NAME.in(schema), equipment(schema));
        });
        registerMob(schema, map, "CaveSpider");
        registerMob(schema, map, "Silverfish");
        registerMob(schema, map, "Blaze");
        registerMob(schema, map, "LavaSlime");
        registerMob(schema, map, "EnderDragon");
        registerMob(schema, map, "WitherBoss");
        registerMob(schema, map, "Bat");
        registerMob(schema, map, "Witch");
        registerMob(schema, map, "Endermite");
        registerMob(schema, map, "Guardian");
        registerMob(schema, map, "Pig");
        registerMob(schema, map, "Sheep");
        registerMob(schema, map, "Cow");
        registerMob(schema, map, "Chicken");
        registerMob(schema, map, "Squid");
        registerMob(schema, map, "Wolf");
        registerMob(schema, map, "MushroomCow");
        registerMob(schema, map, "SnowMan");
        registerMob(schema, map, "Ozelot");
        registerMob(schema, map, "VillagerGolem");
        schema.register(map, "EntityHorse", (string) -> {
            return DSL.optionalFields("Items", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)), "ArmorItem", DataConverterTypes.ITEM_STACK.in(schema), "SaddleItem", DataConverterTypes.ITEM_STACK.in(schema), equipment(schema));
        });
        registerMob(schema, map, "Rabbit");
        schema.register(map, "Villager", (string) -> {
            return DSL.optionalFields("Inventory", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)), "Offers", DSL.optionalFields("Recipes", DSL.list(DSL.optionalFields("buy", DataConverterTypes.ITEM_STACK.in(schema), "buyB", DataConverterTypes.ITEM_STACK.in(schema), "sell", DataConverterTypes.ITEM_STACK.in(schema)))), equipment(schema));
        });
        schema.registerSimple(map, "EnderCrystal");
        schema.registerSimple(map, "AreaEffectCloud");
        schema.registerSimple(map, "ShulkerBullet");
        registerMob(schema, map, "Shulker");
        return map;
    }

    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
        registerInventory(schema, map, "Furnace");
        registerInventory(schema, map, "Chest");
        schema.registerSimple(map, "EnderChest");
        schema.register(map, "RecordPlayer", (string) -> {
            return DSL.optionalFields("RecordItem", DataConverterTypes.ITEM_STACK.in(schema));
        });
        registerInventory(schema, map, "Trap");
        registerInventory(schema, map, "Dropper");
        schema.registerSimple(map, "Sign");
        schema.register(map, "MobSpawner", (string) -> {
            return DataConverterTypes.UNTAGGED_SPAWNER.in(schema);
        });
        schema.registerSimple(map, "Music");
        schema.registerSimple(map, "Piston");
        registerInventory(schema, map, "Cauldron");
        schema.registerSimple(map, "EnchantTable");
        schema.registerSimple(map, "Airportal");
        schema.registerSimple(map, "Control");
        schema.registerSimple(map, "Beacon");
        schema.registerSimple(map, "Skull");
        schema.registerSimple(map, "DLDetector");
        registerInventory(schema, map, "Hopper");
        schema.registerSimple(map, "Comparator");
        schema.register(map, "FlowerPot", (string) -> {
            return DSL.optionalFields("Item", DSL.or(DSL.constType(DSL.intType()), DataConverterTypes.ITEM_NAME.in(schema)));
        });
        schema.registerSimple(map, "Banner");
        schema.registerSimple(map, "Structure");
        schema.registerSimple(map, "EndGateway");
        return map;
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> map, Map<String, Supplier<TypeTemplate>> map2) {
        schema.registerType(false, DataConverterTypes.LEVEL, DSL::remainder);
        schema.registerType(false, DataConverterTypes.PLAYER, () -> {
            return DSL.optionalFields("Inventory", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)), "EnderItems", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)));
        });
        schema.registerType(false, DataConverterTypes.CHUNK, () -> {
            return DSL.fields("Level", DSL.optionalFields("Entities", DSL.list(DataConverterTypes.ENTITY_TREE.in(schema)), "TileEntities", DSL.list(DataConverterTypes.BLOCK_ENTITY.in(schema)), "TileTicks", DSL.list(DSL.fields("i", DataConverterTypes.BLOCK_NAME.in(schema)))));
        });
        schema.registerType(true, DataConverterTypes.BLOCK_ENTITY, () -> {
            return DSL.taggedChoiceLazy("id", DSL.string(), map2);
        });
        schema.registerType(true, DataConverterTypes.ENTITY_TREE, () -> {
            return DSL.optionalFields("Riding", DataConverterTypes.ENTITY_TREE.in(schema), DataConverterTypes.ENTITY.in(schema));
        });
        schema.registerType(false, DataConverterTypes.ENTITY_NAME, () -> {
            return DSL.constType(DataConverterSchemaNamed.namespacedString());
        });
        schema.registerType(true, DataConverterTypes.ENTITY, () -> {
            return DSL.taggedChoiceLazy("id", DSL.string(), map);
        });
        schema.registerType(true, DataConverterTypes.ITEM_STACK, () -> {
            return DSL.hook(DSL.optionalFields("id", DSL.or(DSL.constType(DSL.intType()), DataConverterTypes.ITEM_NAME.in(schema)), "tag", DSL.optionalFields("EntityTag", DataConverterTypes.ENTITY_TREE.in(schema), "BlockEntityTag", DataConverterTypes.BLOCK_ENTITY.in(schema), "CanDestroy", DSL.list(DataConverterTypes.BLOCK_NAME.in(schema)), "CanPlaceOn", DSL.list(DataConverterTypes.BLOCK_NAME.in(schema)), "Items", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)))), ADD_NAMES, HookFunction.IDENTITY);
        });
        schema.registerType(false, DataConverterTypes.OPTIONS, DSL::remainder);
        schema.registerType(false, DataConverterTypes.BLOCK_NAME, () -> {
            return DSL.or(DSL.constType(DSL.intType()), DSL.constType(DataConverterSchemaNamed.namespacedString()));
        });
        schema.registerType(false, DataConverterTypes.ITEM_NAME, () -> {
            return DSL.constType(DataConverterSchemaNamed.namespacedString());
        });
        schema.registerType(false, DataConverterTypes.STATS, DSL::remainder);
        schema.registerType(false, DataConverterTypes.SAVED_DATA, () -> {
            return DSL.optionalFields("data", DSL.optionalFields("Features", DSL.compoundList(DataConverterTypes.STRUCTURE_FEATURE.in(schema)), "Objectives", DSL.list(DataConverterTypes.OBJECTIVE.in(schema)), "Teams", DSL.list(DataConverterTypes.TEAM.in(schema))));
        });
        schema.registerType(false, DataConverterTypes.STRUCTURE_FEATURE, DSL::remainder);
        schema.registerType(false, DataConverterTypes.OBJECTIVE, DSL::remainder);
        schema.registerType(false, DataConverterTypes.TEAM, DSL::remainder);
        schema.registerType(true, DataConverterTypes.UNTAGGED_SPAWNER, DSL::remainder);
        schema.registerType(false, DataConverterTypes.POI_CHUNK, DSL::remainder);
        schema.registerType(true, DataConverterTypes.WORLD_GEN_SETTINGS, DSL::remainder);
        schema.registerType(false, DataConverterTypes.ENTITY_CHUNK, () -> {
            return DSL.optionalFields("Entities", DSL.list(DataConverterTypes.ENTITY_TREE.in(schema)));
        });
    }

    protected static <T> T addNames(Dynamic<T> dynamic, Map<String, String> map, String string) {
        return dynamic.update("tag", (dynamic2) -> {
            return dynamic2.update("BlockEntityTag", (dynamic2x) -> {
                String string = dynamic.get("id").asString().result().map(DataConverterSchemaNamed::ensureNamespaced).orElse("minecraft:air");
                if (!"minecraft:air".equals(string)) {
                    String string2 = map.get(string);
                    if (string2 != null) {
                        return dynamic2x.set("id", dynamic.createString(string2));
                    }

                    LOGGER.warn("Unable to resolve BlockEntity for ItemStack: {}", (Object)string);
                }

                return dynamic2x;
            }).update("EntityTag", (dynamic2x) -> {
                String string2 = dynamic.get("id").asString("");
                return "minecraft:armor_stand".equals(DataConverterSchemaNamed.ensureNamespaced(string2)) ? dynamic2x.set("id", dynamic.createString(string)) : dynamic2x;
            });
        }).getValue();
    }
}
