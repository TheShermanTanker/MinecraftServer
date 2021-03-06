package net.minecraft.world.level.levelgen.blending;

import com.google.common.primitives.Doubles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.EnumDirection8;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.RegionLimitedWorldAccess;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.HeightMap;

public class BlendingData {
    private static final double BLENDING_DENSITY_FACTOR = 0.1D;
    protected static final IWorldHeightAccess AREA_WITH_OLD_GENERATION = new IWorldHeightAccess() {
        @Override
        public int getHeight() {
            return 256;
        }

        @Override
        public int getMinBuildHeight() {
            return 0;
        }
    };
    protected static final int CELL_WIDTH = 4;
    protected static final int CELL_HEIGHT = 8;
    protected static final int CELL_RATIO = 2;
    private static final int CELLS_PER_SECTION_Y = 2;
    private static final int QUARTS_PER_SECTION = QuartPos.fromBlock(16);
    private static final int CELL_HORIZONTAL_MAX_INDEX_INSIDE = QUARTS_PER_SECTION - 1;
    private static final int CELL_HORIZONTAL_MAX_INDEX_OUTSIDE = QUARTS_PER_SECTION;
    private static final int CELL_COLUMN_INSIDE_COUNT = 2 * CELL_HORIZONTAL_MAX_INDEX_INSIDE + 1;
    private static final int CELL_COLUMN_OUTSIDE_COUNT = 2 * CELL_HORIZONTAL_MAX_INDEX_OUTSIDE + 1;
    private static final int CELL_COLUMN_COUNT = CELL_COLUMN_INSIDE_COUNT + CELL_COLUMN_OUTSIDE_COUNT;
    private static final int CELL_HORIZONTAL_FLOOR_COUNT = QUARTS_PER_SECTION + 1;
    private static final List<Block> SURFACE_BLOCKS = List.of(Blocks.PODZOL, Blocks.GRAVEL, Blocks.GRASS_BLOCK, Blocks.STONE, Blocks.COARSE_DIRT, Blocks.SAND, Blocks.RED_SAND, Blocks.MYCELIUM, Blocks.SNOW_BLOCK, Blocks.TERRACOTTA, Blocks.DIRT);
    protected static final double NO_VALUE = Double.MAX_VALUE;
    private final boolean oldNoise;
    private boolean hasCalculatedData;
    private final double[] heights;
    private final transient double[][] densities;
    private final transient double[] floorDensities;
    private static final Codec<double[]> DOUBLE_ARRAY_CODEC = Codec.DOUBLE.listOf().xmap(Doubles::toArray, Doubles::asList);
    public static final Codec<BlendingData> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.BOOL.fieldOf("old_noise").forGetter(BlendingData::oldNoise), DOUBLE_ARRAY_CODEC.optionalFieldOf("heights").forGetter((blendingData) -> {
            return DoubleStream.of(blendingData.heights).anyMatch((d) -> {
                return d != Double.MAX_VALUE;
            }) ? Optional.of(blendingData.heights) : Optional.empty();
        })).apply(instance, BlendingData::new);
    }).comapFlatMap(BlendingData::validateArraySize, Function.identity());

    private static DataResult<BlendingData> validateArraySize(BlendingData blendingData) {
        return blendingData.heights.length != CELL_COLUMN_COUNT ? DataResult.error("heights has to be of length " + CELL_COLUMN_COUNT) : DataResult.success(blendingData);
    }

    private BlendingData(boolean oldNoise, Optional<double[]> optional) {
        this.oldNoise = oldNoise;
        this.heights = optional.orElse(SystemUtils.make(new double[CELL_COLUMN_COUNT], (ds) -> {
            Arrays.fill(ds, Double.MAX_VALUE);
        }));
        this.densities = new double[CELL_COLUMN_COUNT][];
        this.floorDensities = new double[CELL_HORIZONTAL_FLOOR_COUNT * CELL_HORIZONTAL_FLOOR_COUNT];
    }

    public boolean oldNoise() {
        return this.oldNoise;
    }

    @Nullable
    public static BlendingData getOrUpdateBlendingData(RegionLimitedWorldAccess chunkRegion, int chunkX, int chunkZ) {
        IChunkAccess chunkAccess = chunkRegion.getChunkAt(chunkX, chunkZ);
        BlendingData blendingData = chunkAccess.getBlendingData();
        if (blendingData != null && blendingData.oldNoise()) {
            blendingData.calculateData(chunkAccess, sideByGenerationAge(chunkRegion, chunkX, chunkZ, false));
            return blendingData;
        } else {
            return null;
        }
    }

    public static Set<EnumDirection8> sideByGenerationAge(GeneratorAccessSeed access, int chunkX, int chunkZ, boolean newNoise) {
        Set<EnumDirection8> set = EnumSet.noneOf(EnumDirection8.class);

        for(EnumDirection8 direction8 : EnumDirection8.values()) {
            int i = chunkX;
            int j = chunkZ;

            for(EnumDirection direction : direction8.getDirections()) {
                i += direction.getAdjacentX();
                j += direction.getAdjacentZ();
            }

            if (access.getChunkAt(i, j).isOldNoiseGeneration() == newNoise) {
                set.add(direction8);
            }
        }

        return set;
    }

    private void calculateData(IChunkAccess chunkAccess, Set<EnumDirection8> set) {
        if (!this.hasCalculatedData) {
            Arrays.fill(this.floorDensities, 1.0D);
            if (set.contains(EnumDirection8.NORTH) || set.contains(EnumDirection8.WEST) || set.contains(EnumDirection8.NORTH_WEST)) {
                this.addValuesForColumn(getInsideIndex(0, 0), chunkAccess, 0, 0);
            }

            if (set.contains(EnumDirection8.NORTH)) {
                for(int i = 1; i < QUARTS_PER_SECTION; ++i) {
                    this.addValuesForColumn(getInsideIndex(i, 0), chunkAccess, 4 * i, 0);
                }
            }

            if (set.contains(EnumDirection8.WEST)) {
                for(int j = 1; j < QUARTS_PER_SECTION; ++j) {
                    this.addValuesForColumn(getInsideIndex(0, j), chunkAccess, 0, 4 * j);
                }
            }

            if (set.contains(EnumDirection8.EAST)) {
                for(int k = 1; k < QUARTS_PER_SECTION; ++k) {
                    this.addValuesForColumn(getOutsideIndex(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE, k), chunkAccess, 15, 4 * k);
                }
            }

            if (set.contains(EnumDirection8.SOUTH)) {
                for(int l = 0; l < QUARTS_PER_SECTION; ++l) {
                    this.addValuesForColumn(getOutsideIndex(l, CELL_HORIZONTAL_MAX_INDEX_OUTSIDE), chunkAccess, 4 * l, 15);
                }
            }

            if (set.contains(EnumDirection8.EAST) && set.contains(EnumDirection8.NORTH_EAST)) {
                this.addValuesForColumn(getOutsideIndex(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE, 0), chunkAccess, 15, 0);
            }

            if (set.contains(EnumDirection8.EAST) && set.contains(EnumDirection8.SOUTH) && set.contains(EnumDirection8.SOUTH_EAST)) {
                this.addValuesForColumn(getOutsideIndex(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE, CELL_HORIZONTAL_MAX_INDEX_OUTSIDE), chunkAccess, 15, 15);
            }

            this.hasCalculatedData = true;
        }
    }

    private void addValuesForColumn(int index, IChunkAccess chunk, int x, int z) {
        if (this.heights[index] == Double.MAX_VALUE) {
            this.heights[index] = (double)getHeightAtXZ(chunk, x, z);
        }

        this.densities[index] = getDensityColumn(chunk, x, z, MathHelper.floor(this.heights[index]));
    }

    private static int getHeightAtXZ(IChunkAccess chunk, int x, int z) {
        int i;
        if (chunk.hasPrimedHeightmap(HeightMap.Type.WORLD_SURFACE_WG)) {
            i = Math.min(chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE_WG, x, z) + 1, AREA_WITH_OLD_GENERATION.getMaxBuildHeight());
        } else {
            i = AREA_WITH_OLD_GENERATION.getMaxBuildHeight();
        }

        int k = AREA_WITH_OLD_GENERATION.getMinBuildHeight();
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(x, i, z);

        while(mutableBlockPos.getY() > k) {
            mutableBlockPos.move(EnumDirection.DOWN);
            if (SURFACE_BLOCKS.contains(chunk.getType(mutableBlockPos).getBlock())) {
                return mutableBlockPos.getY();
            }
        }

        return k;
    }

    private static double read1(IChunkAccess chunkAccess, BlockPosition.MutableBlockPosition mutableBlockPos) {
        return isGround(chunkAccess, mutableBlockPos.move(EnumDirection.DOWN)) ? 1.0D : -1.0D;
    }

    private static double read7(IChunkAccess chunkAccess, BlockPosition.MutableBlockPosition mutableBlockPos) {
        double d = 0.0D;

        for(int i = 0; i < 7; ++i) {
            d += read1(chunkAccess, mutableBlockPos);
        }

        return d;
    }

    private static double[] getDensityColumn(IChunkAccess chunk, int x, int z, int i) {
        double[] ds = new double[cellCountPerColumn()];
        Arrays.fill(ds, -1.0D);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(x, AREA_WITH_OLD_GENERATION.getMaxBuildHeight(), z);
        double d = read7(chunk, mutableBlockPos);

        for(int j = ds.length - 2; j >= 0; --j) {
            double e = read1(chunk, mutableBlockPos);
            double f = read7(chunk, mutableBlockPos);
            ds[j] = (d + e + f) / 15.0D;
            d = f;
        }

        int k = MathHelper.intFloorDiv(i, 8);
        if (k >= 1 && k < ds.length) {
            double g = ((double)i + 0.5D) % 8.0D / 8.0D;
            double h = (1.0D - g) / g;
            double l = Math.max(h, 1.0D) * 0.25D;
            ds[k] = -h / l;
            ds[k - 1] = 1.0D / l;
        }

        return ds;
    }

    private static boolean isGround(IChunkAccess chunk, BlockPosition pos) {
        IBlockData blockState = chunk.getType(pos);
        if (blockState.isAir()) {
            return false;
        } else if (blockState.is(TagsBlock.LEAVES)) {
            return false;
        } else if (blockState.is(TagsBlock.LOGS)) {
            return false;
        } else if (!blockState.is(Blocks.BROWN_MUSHROOM_BLOCK) && !blockState.is(Blocks.RED_MUSHROOM_BLOCK)) {
            return !blockState.getCollisionShape(chunk, pos).isEmpty();
        } else {
            return false;
        }
    }

    protected double getHeight(int i, int j, int k) {
        if (i != CELL_HORIZONTAL_MAX_INDEX_OUTSIDE && k != CELL_HORIZONTAL_MAX_INDEX_OUTSIDE) {
            return i != 0 && k != 0 ? Double.MAX_VALUE : this.heights[getInsideIndex(i, k)];
        } else {
            return this.heights[getOutsideIndex(i, k)];
        }
    }

    private static double getDensity(@Nullable double[] ds, int i) {
        if (ds == null) {
            return Double.MAX_VALUE;
        } else {
            int j = i - getColumnMinY();
            return j >= 0 && j < ds.length ? ds[j] * 0.1D : Double.MAX_VALUE;
        }
    }

    protected double getDensity(int i, int j, int k) {
        if (j == getMinY()) {
            return this.floorDensities[this.getFloorIndex(i, k)] * 0.1D;
        } else if (i != CELL_HORIZONTAL_MAX_INDEX_OUTSIDE && k != CELL_HORIZONTAL_MAX_INDEX_OUTSIDE) {
            return i != 0 && k != 0 ? Double.MAX_VALUE : getDensity(this.densities[getInsideIndex(i, k)], j);
        } else {
            return getDensity(this.densities[getOutsideIndex(i, k)], j);
        }
    }

    protected void iterateHeights(int i, int j, BlendingData.HeightConsumer heightConsumer) {
        for(int k = 0; k < this.densities.length; ++k) {
            double d = this.heights[k];
            if (d != Double.MAX_VALUE) {
                heightConsumer.consume(i + getX(k), j + getZ(k), d);
            }
        }

    }

    protected void iterateDensities(int i, int j, int k, int l, BlendingData.DensityConsumer densityConsumer) {
        int m = getColumnMinY();
        int n = Math.max(0, k - m);
        int o = Math.min(cellCountPerColumn(), l - m);

        for(int p = 0; p < this.densities.length; ++p) {
            double[] ds = this.densities[p];
            if (ds != null) {
                int q = i + getX(p);
                int r = j + getZ(p);

                for(int s = n; s < o; ++s) {
                    densityConsumer.consume(q, s + m, r, ds[s] * 0.1D);
                }
            }
        }

        if (m >= k && m <= l) {
            for(int t = 0; t < this.floorDensities.length; ++t) {
                int u = this.getFloorX(t);
                int v = this.getFloorZ(t);
                densityConsumer.consume(u, m, v, this.floorDensities[t] * 0.1D);
            }
        }

    }

    private int getFloorIndex(int i, int j) {
        return i * CELL_HORIZONTAL_FLOOR_COUNT + j;
    }

    private int getFloorX(int i) {
        return i / CELL_HORIZONTAL_FLOOR_COUNT;
    }

    private int getFloorZ(int i) {
        return i % CELL_HORIZONTAL_FLOOR_COUNT;
    }

    private static int cellCountPerColumn() {
        return AREA_WITH_OLD_GENERATION.getSectionsCount() * 2;
    }

    private static int getColumnMinY() {
        return getMinY() + 1;
    }

    private static int getMinY() {
        return AREA_WITH_OLD_GENERATION.getMinSection() * 2;
    }

    private static int getInsideIndex(int i, int j) {
        return CELL_HORIZONTAL_MAX_INDEX_INSIDE - i + j;
    }

    private static int getOutsideIndex(int i, int j) {
        return CELL_COLUMN_INSIDE_COUNT + i + CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - j;
    }

    private static int getX(int i) {
        if (i < CELL_COLUMN_INSIDE_COUNT) {
            return zeroIfNegative(CELL_HORIZONTAL_MAX_INDEX_INSIDE - i);
        } else {
            int j = i - CELL_COLUMN_INSIDE_COUNT;
            return CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - zeroIfNegative(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - j);
        }
    }

    private static int getZ(int i) {
        if (i < CELL_COLUMN_INSIDE_COUNT) {
            return zeroIfNegative(i - CELL_HORIZONTAL_MAX_INDEX_INSIDE);
        } else {
            int j = i - CELL_COLUMN_INSIDE_COUNT;
            return CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - zeroIfNegative(j - CELL_HORIZONTAL_MAX_INDEX_OUTSIDE);
        }
    }

    private static int zeroIfNegative(int i) {
        return i & ~(i >> 31);
    }

    protected interface DensityConsumer {
        void consume(int i, int j, int k, double d);
    }

    protected interface HeightConsumer {
        void consume(int i, int j, double d);
    }
}
