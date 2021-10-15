package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public class DefinedStructureTestBlock extends DefinedStructureRuleTest {
    public static final Codec<DefinedStructureTestBlock> CODEC = IRegistry.BLOCK.fieldOf("block").xmap(DefinedStructureTestBlock::new, (blockMatchTest) -> {
        return blockMatchTest.block;
    }).codec();
    private final Block block;

    public DefinedStructureTestBlock(Block block) {
        this.block = block;
    }

    @Override
    public boolean test(IBlockData state, Random random) {
        return state.is(this.block);
    }

    @Override
    protected DefinedStructureRuleTestType<?> getType() {
        return DefinedStructureRuleTestType.BLOCK_TEST;
    }
}
