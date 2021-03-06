package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public abstract class HeightProvider {
    private static final Codec<Either<VerticalAnchor, HeightProvider>> CONSTANT_OR_DISPATCH_CODEC = Codec.either(VerticalAnchor.CODEC, IRegistry.HEIGHT_PROVIDER_TYPES.byNameCodec().dispatch(HeightProvider::getType, HeightProviderType::codec));
    public static final Codec<HeightProvider> CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap((either) -> {
        return either.map(ConstantHeight::of, (provider) -> {
            return provider;
        });
    }, (provider) -> {
        return provider.getType() == HeightProviderType.CONSTANT ? Either.left(((ConstantHeight)provider).getValue()) : Either.right(provider);
    });

    public abstract int sample(Random random, WorldGenerationContext context);

    public abstract HeightProviderType<?> getType();
}
