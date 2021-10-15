package net.minecraft.world.level.lighting;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.chunk.ILightAccess;
import net.minecraft.world.level.chunk.NibbleArray;

public class LightEngine implements ILightEngine {
    public static final int MAX_SOURCE_LEVEL = 15;
    public static final int LIGHT_SECTION_PADDING = 1;
    protected final IWorldHeightAccess levelHeightAccessor;
    @Nullable
    private final LightEngineLayer<?, ?> blockEngine;
    @Nullable
    private final LightEngineLayer<?, ?> skyEngine;

    public LightEngine(ILightAccess chunkProvider, boolean hasBlockLight, boolean hasSkyLight) {
        this.levelHeightAccessor = chunkProvider.getWorld();
        this.blockEngine = hasBlockLight ? new LightEngineBlock(chunkProvider) : null;
        this.skyEngine = hasSkyLight ? new LightEngineSky(chunkProvider) : null;
    }

    @Override
    public void checkBlock(BlockPosition pos) {
        if (this.blockEngine != null) {
            this.blockEngine.checkBlock(pos);
        }

        if (this.skyEngine != null) {
            this.skyEngine.checkBlock(pos);
        }

    }

    @Override
    public void onBlockEmissionIncrease(BlockPosition pos, int level) {
        if (this.blockEngine != null) {
            this.blockEngine.onBlockEmissionIncrease(pos, level);
        }

    }

    @Override
    public boolean hasLightWork() {
        if (this.skyEngine != null && this.skyEngine.hasLightWork()) {
            return true;
        } else {
            return this.blockEngine != null && this.blockEngine.hasLightWork();
        }
    }

    @Override
    public int runUpdates(int i, boolean bl, boolean bl2) {
        if (this.blockEngine != null && this.skyEngine != null) {
            int j = i / 2;
            int k = this.blockEngine.runUpdates(j, bl, bl2);
            int l = i - j + k;
            int m = this.skyEngine.runUpdates(l, bl, bl2);
            return k == 0 && m > 0 ? this.blockEngine.runUpdates(m, bl, bl2) : m;
        } else if (this.blockEngine != null) {
            return this.blockEngine.runUpdates(i, bl, bl2);
        } else {
            return this.skyEngine != null ? this.skyEngine.runUpdates(i, bl, bl2) : i;
        }
    }

    @Override
    public void updateSectionStatus(SectionPosition pos, boolean notReady) {
        if (this.blockEngine != null) {
            this.blockEngine.updateSectionStatus(pos, notReady);
        }

        if (this.skyEngine != null) {
            this.skyEngine.updateSectionStatus(pos, notReady);
        }

    }

    @Override
    public void enableLightSources(ChunkCoordIntPair chunkPos, boolean bl) {
        if (this.blockEngine != null) {
            this.blockEngine.enableLightSources(chunkPos, bl);
        }

        if (this.skyEngine != null) {
            this.skyEngine.enableLightSources(chunkPos, bl);
        }

    }

    public LightEngineLayerEventListener getLayerListener(EnumSkyBlock lightType) {
        if (lightType == EnumSkyBlock.BLOCK) {
            return (LightEngineLayerEventListener)(this.blockEngine == null ? LightEngineLayerEventListener.Void.INSTANCE : this.blockEngine);
        } else {
            return (LightEngineLayerEventListener)(this.skyEngine == null ? LightEngineLayerEventListener.Void.INSTANCE : this.skyEngine);
        }
    }

    public String getDebugData(EnumSkyBlock lightType, SectionPosition sectionPos) {
        if (lightType == EnumSkyBlock.BLOCK) {
            if (this.blockEngine != null) {
                return this.blockEngine.getDebugData(sectionPos.asLong());
            }
        } else if (this.skyEngine != null) {
            return this.skyEngine.getDebugData(sectionPos.asLong());
        }

        return "n/a";
    }

    public void queueSectionData(EnumSkyBlock lightType, SectionPosition pos, @Nullable NibbleArray nibbles, boolean bl) {
        if (lightType == EnumSkyBlock.BLOCK) {
            if (this.blockEngine != null) {
                this.blockEngine.queueSectionData(pos.asLong(), nibbles, bl);
            }
        } else if (this.skyEngine != null) {
            this.skyEngine.queueSectionData(pos.asLong(), nibbles, bl);
        }

    }

    public void retainData(ChunkCoordIntPair pos, boolean retainData) {
        if (this.blockEngine != null) {
            this.blockEngine.retainData(pos, retainData);
        }

        if (this.skyEngine != null) {
            this.skyEngine.retainData(pos, retainData);
        }

    }

    public int getRawBrightness(BlockPosition pos, int ambientDarkness) {
        int i = this.skyEngine == null ? 0 : this.skyEngine.getLightValue(pos) - ambientDarkness;
        int j = this.blockEngine == null ? 0 : this.blockEngine.getLightValue(pos);
        return Math.max(j, i);
    }

    public int getLightSectionCount() {
        return this.levelHeightAccessor.getSectionsCount() + 2;
    }

    public int getMinLightSection() {
        return this.levelHeightAccessor.getMinSection() - 1;
    }

    public int getMaxLightSection() {
        return this.getMinLightSection() + this.getLightSectionCount();
    }
}
