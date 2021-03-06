package net.minecraft.network.chat;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;

public final class ChatHexColor {
    private static final String CUSTOM_COLOR_PREFIX = "#";
    private static final Map<EnumChatFormat, ChatHexColor> LEGACY_FORMAT_TO_COLOR = Stream.of(EnumChatFormat.values()).filter(EnumChatFormat::isColor).collect(ImmutableMap.toImmutableMap(Function.identity(), (formatting) -> {
        return new ChatHexColor(formatting.getColor(), formatting.getName());
    }));
    private static final Map<String, ChatHexColor> NAMED_COLORS = LEGACY_FORMAT_TO_COLOR.values().stream().collect(ImmutableMap.toImmutableMap((textColor) -> {
        return textColor.name;
    }, Function.identity()));
    private final int value;
    @Nullable
    public final String name;

    private ChatHexColor(int rgb, String name) {
        this.value = rgb;
        this.name = name;
    }

    private ChatHexColor(int rgb) {
        this.value = rgb;
        this.name = null;
    }

    public int getValue() {
        return this.value;
    }

    public String serialize() {
        return this.name != null ? this.name : this.formatValue();
    }

    private String formatValue() {
        return String.format("#%06X", this.value);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            ChatHexColor textColor = (ChatHexColor)object;
            return this.value == textColor.value;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.name);
    }

    @Override
    public String toString() {
        return this.name != null ? this.name : this.formatValue();
    }

    @Nullable
    public static ChatHexColor fromLegacyFormat(EnumChatFormat formatting) {
        return LEGACY_FORMAT_TO_COLOR.get(formatting);
    }

    public static ChatHexColor fromRgb(int rgb) {
        return new ChatHexColor(rgb);
    }

    @Nullable
    public static ChatHexColor parseColor(String name) {
        if (name.startsWith("#")) {
            try {
                int i = Integer.parseInt(name.substring(1), 16);
                return fromRgb(i);
            } catch (NumberFormatException var2) {
                return null;
            }
        } else {
            return NAMED_COLORS.get(name);
        }
    }
}
