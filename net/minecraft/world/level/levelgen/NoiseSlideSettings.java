package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public class NoiseSlideSettings {
    public static final Codec<NoiseSlideSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.INT.fieldOf("target").forGetter(NoiseSlideSettings::target), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("size").forGetter(NoiseSlideSettings::size), Codec.INT.fieldOf("offset").forGetter(NoiseSlideSettings::offset)).apply(instance, NoiseSlideSettings::new);
    });
    private final int target;
    private final int size;
    private final int offset;

    public NoiseSlideSettings(int target, int size, int offset) {
        this.target = target;
        this.size = size;
        this.offset = offset;
    }

    public int target() {
        return this.target;
    }

    public int size() {
        return this.size;
    }

    public int offset() {
        return this.offset;
    }
}