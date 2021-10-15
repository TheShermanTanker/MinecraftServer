package net.minecraft.world.level.block.state.predicate;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Material;

public class MaterialPredicate implements Predicate<IBlockData> {
    private static final MaterialPredicate AIR = new MaterialPredicate(Material.AIR) {
        @Override
        public boolean test(@Nullable IBlockData blockState) {
            return blockState != null && blockState.isAir();
        }
    };
    private final Material material;

    MaterialPredicate(Material material) {
        this.material = material;
    }

    public static MaterialPredicate forMaterial(Material material) {
        return material == Material.AIR ? AIR : new MaterialPredicate(material);
    }

    @Override
    public boolean test(@Nullable IBlockData blockState) {
        return blockState != null && blockState.getMaterial() == this.material;
    }
}
