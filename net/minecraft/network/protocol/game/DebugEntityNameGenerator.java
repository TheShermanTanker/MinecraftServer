package net.minecraft.network.protocol.game;

import java.util.Random;
import java.util.UUID;
import net.minecraft.SystemUtils;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;

public class DebugEntityNameGenerator {
    private static final String[] NAMES_FIRST_PART = new String[]{"Slim", "Far", "River", "Silly", "Fat", "Thin", "Fish", "Bat", "Dark", "Oak", "Sly", "Bush", "Zen", "Bark", "Cry", "Slack", "Soup", "Grim", "Hook", "Dirt", "Mud", "Sad", "Hard", "Crook", "Sneak", "Stink", "Weird", "Fire", "Soot", "Soft", "Rough", "Cling", "Scar"};
    private static final String[] NAMES_SECOND_PART = new String[]{"Fox", "Tail", "Jaw", "Whisper", "Twig", "Root", "Finder", "Nose", "Brow", "Blade", "Fry", "Seek", "Wart", "Tooth", "Foot", "Leaf", "Stone", "Fall", "Face", "Tongue", "Voice", "Lip", "Mouth", "Snail", "Toe", "Ear", "Hair", "Beard", "Shirt", "Fist"};

    public static String getEntityName(Entity entity) {
        if (entity instanceof EntityHuman) {
            return entity.getDisplayName().getString();
        } else {
            IChatBaseComponent component = entity.getCustomName();
            return component != null ? component.getString() : getEntityName(entity.getUniqueID());
        }
    }

    public static String getEntityName(UUID uuid) {
        Random random = getRandom(uuid);
        return getRandomString(random, NAMES_FIRST_PART) + getRandomString(random, NAMES_SECOND_PART);
    }

    private static String getRandomString(Random random, String[] options) {
        return SystemUtils.getRandom(options, random);
    }

    private static Random getRandom(UUID uuid) {
        return new Random((long)(uuid.hashCode() >> 2));
    }
}
