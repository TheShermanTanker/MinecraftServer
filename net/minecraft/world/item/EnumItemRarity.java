package net.minecraft.world.item;

import net.minecraft.EnumChatFormat;

public enum EnumItemRarity {
    COMMON(EnumChatFormat.WHITE),
    UNCOMMON(EnumChatFormat.YELLOW),
    RARE(EnumChatFormat.AQUA),
    EPIC(EnumChatFormat.LIGHT_PURPLE);

    public final EnumChatFormat color;

    private EnumItemRarity(EnumChatFormat formatting) {
        this.color = formatting;
    }
}
