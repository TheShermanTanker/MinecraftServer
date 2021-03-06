package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.util.MathHelper;

public class IntProviderClamped extends IntProvider {
    public static final Codec<IntProviderClamped> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IntProvider.CODEC.fieldOf("source").forGetter((provider) -> {
            return provider.source;
        }), Codec.INT.fieldOf("min_inclusive").forGetter((provider) -> {
            return provider.minInclusive;
        }), Codec.INT.fieldOf("max_inclusive").forGetter((provider) -> {
            return provider.maxInclusive;
        })).apply(instance, IntProviderClamped::new);
    }).comapFlatMap((provider) -> {
        return provider.maxInclusive < provider.minInclusive ? DataResult.error("Max must be at least min, min_inclusive: " + provider.minInclusive + ", max_inclusive: " + provider.maxInclusive) : DataResult.success(provider);
    }, Function.identity());
    private final IntProvider source;
    private int minInclusive;
    private int maxInclusive;

    public static IntProviderClamped of(IntProvider source, int min, int max) {
        return new IntProviderClamped(source, min, max);
    }

    public IntProviderClamped(IntProvider source, int min, int max) {
        this.source = source;
        this.minInclusive = min;
        this.maxInclusive = max;
    }

    @Override
    public int sample(Random random) {
        return MathHelper.clamp(this.source.sample(random), this.minInclusive, this.maxInclusive);
    }

    @Override
    public int getMinValue() {
        return Math.max(this.minInclusive, this.source.getMinValue());
    }

    @Override
    public int getMaxValue() {
        return Math.min(this.maxInclusive, this.source.getMaxValue());
    }

    @Override
    public IntProviderType<?> getType() {
        return IntProviderType.CLAMPED;
    }
}
