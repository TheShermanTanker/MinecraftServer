package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BiasedToBottomHeight extends HeightProvider {
    public static final Codec<BiasedToBottomHeight> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter((biasedToBottomHeight) -> {
            return biasedToBottomHeight.minInclusive;
        }), VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter((biasedToBottomHeight) -> {
            return biasedToBottomHeight.maxInclusive;
        }), Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("inner", 1).forGetter((biasedToBottomHeight) -> {
            return biasedToBottomHeight.inner;
        })).apply(instance, BiasedToBottomHeight::new);
    });
    private static final Logger LOGGER = LogManager.getLogger();
    private final VerticalAnchor minInclusive;
    private final VerticalAnchor maxInclusive;
    private final int inner;

    private BiasedToBottomHeight(VerticalAnchor minOffset, VerticalAnchor maxOffset, int inner) {
        this.minInclusive = minOffset;
        this.maxInclusive = maxOffset;
        this.inner = inner;
    }

    public static BiasedToBottomHeight of(VerticalAnchor minOffset, VerticalAnchor maxOffset, int inner) {
        return new BiasedToBottomHeight(minOffset, maxOffset, inner);
    }

    @Override
    public int sample(Random random, WorldGenerationContext context) {
        int i = this.minInclusive.resolveY(context);
        int j = this.maxInclusive.resolveY(context);
        if (j - i - this.inner + 1 <= 0) {
            LOGGER.warn("Empty height range: {}", (Object)this);
            return i;
        } else {
            int k = random.nextInt(j - i - this.inner + 1);
            return random.nextInt(k + this.inner) + i;
        }
    }

    @Override
    public HeightProviderType<?> getType() {
        return HeightProviderType.BIASED_TO_BOTTOM;
    }

    @Override
    public String toString() {
        return "biased[" + this.minInclusive + "-" + this.maxInclusive + " inner: " + this.inner + "]";
    }
}
