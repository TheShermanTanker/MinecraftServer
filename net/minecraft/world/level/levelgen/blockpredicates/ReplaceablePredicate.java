package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.world.level.block.state.IBlockData;

class ReplaceablePredicate extends StateTestingPredicate {
    public static final Codec<ReplaceablePredicate> CODEC = RecordCodecBuilder.create((instance) -> {
        return stateTestingCodec(instance).apply(instance, ReplaceablePredicate::new);
    });

    public ReplaceablePredicate(BaseBlockPosition offset) {
        super(offset);
    }

    @Override
    protected boolean test(IBlockData state) {
        return state.getMaterial().isReplaceable();
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.REPLACEABLE;
    }
}
