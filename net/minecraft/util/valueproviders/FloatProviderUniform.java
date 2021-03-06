package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.util.MathHelper;

public class FloatProviderUniform extends FloatProvider {
    public static final Codec<FloatProviderUniform> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.FLOAT.fieldOf("min_inclusive").forGetter((provider) -> {
            return provider.minInclusive;
        }), Codec.FLOAT.fieldOf("max_exclusive").forGetter((provider) -> {
            return provider.maxExclusive;
        })).apply(instance, FloatProviderUniform::new);
    }).comapFlatMap((provider) -> {
        return provider.maxExclusive <= provider.minInclusive ? DataResult.error("Max must be larger than min, min_inclusive: " + provider.minInclusive + ", max_exclusive: " + provider.maxExclusive) : DataResult.success(provider);
    }, Function.identity());
    private final float minInclusive;
    private final float maxExclusive;

    private FloatProviderUniform(float min, float max) {
        this.minInclusive = min;
        this.maxExclusive = max;
    }

    public static FloatProviderUniform of(float min, float max) {
        if (max <= min) {
            throw new IllegalArgumentException("Max must exceed min");
        } else {
            return new FloatProviderUniform(min, max);
        }
    }

    @Override
    public float sample(Random random) {
        return MathHelper.randomBetween(random, this.minInclusive, this.maxExclusive);
    }

    @Override
    public float getMinValue() {
        return this.minInclusive;
    }

    @Override
    public float getMaxValue() {
        return this.maxExclusive;
    }

    @Override
    public FloatProviderType<?> getType() {
        return FloatProviderType.UNIFORM;
    }

    @Override
    public String toString() {
        return "[" + this.minInclusive + "-" + this.maxExclusive + "]";
    }
}
