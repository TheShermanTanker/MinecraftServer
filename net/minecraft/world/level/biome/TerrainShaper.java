package net.minecraft.world.level.biome;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.INamable;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.util.VisibleForDebug;

public final class TerrainShaper {
    private static final Codec<CubicSpline<TerrainShaper.Point>> SPLINE_CODEC = CubicSpline.codec(TerrainShaper.Coordinate.WIDE_CODEC);
    public static final Codec<TerrainShaper> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(SPLINE_CODEC.fieldOf("offset").forGetter(TerrainShaper::offsetSampler), SPLINE_CODEC.fieldOf("factor").forGetter(TerrainShaper::factorSampler), SPLINE_CODEC.fieldOf("jaggedness").forGetter((terrainShaper) -> {
            return terrainShaper.jaggednessSampler;
        })).apply(instance, TerrainShaper::new);
    });
    private static final float GLOBAL_OFFSET = -0.50375F;
    private static final ToFloatFunction<Float> NO_TRANSFORM = (float_) -> {
        return float_;
    };
    private final CubicSpline<TerrainShaper.Point> offsetSampler;
    private final CubicSpline<TerrainShaper.Point> factorSampler;
    private final CubicSpline<TerrainShaper.Point> jaggednessSampler;

    public TerrainShaper(CubicSpline<TerrainShaper.Point> offsetSpline, CubicSpline<TerrainShaper.Point> factorSpline, CubicSpline<TerrainShaper.Point> peakSpline) {
        this.offsetSampler = offsetSpline;
        this.factorSampler = factorSpline;
        this.jaggednessSampler = peakSpline;
    }

    private static float getAmplifiedOffset(float f) {
        return f < 0.0F ? f : f * 2.0F;
    }

    private static float getAmplifiedFactor(float f) {
        return 1.25F - 6.25F / (f + 5.0F);
    }

    private static float getAmplifiedJaggedness(float f) {
        return f * 2.0F;
    }

    public static TerrainShaper overworld(boolean amplified) {
        ToFloatFunction<Float> toFloatFunction = amplified ? TerrainShaper::getAmplifiedOffset : NO_TRANSFORM;
        ToFloatFunction<Float> toFloatFunction2 = amplified ? TerrainShaper::getAmplifiedFactor : NO_TRANSFORM;
        ToFloatFunction<Float> toFloatFunction3 = amplified ? TerrainShaper::getAmplifiedJaggedness : NO_TRANSFORM;
        CubicSpline<TerrainShaper.Point> cubicSpline = buildErosionOffsetSpline(-0.15F, 0.0F, 0.0F, 0.1F, 0.0F, -0.03F, false, false, toFloatFunction);
        CubicSpline<TerrainShaper.Point> cubicSpline2 = buildErosionOffsetSpline(-0.1F, 0.03F, 0.1F, 0.1F, 0.01F, -0.03F, false, false, toFloatFunction);
        CubicSpline<TerrainShaper.Point> cubicSpline3 = buildErosionOffsetSpline(-0.1F, 0.03F, 0.1F, 0.7F, 0.01F, -0.03F, true, true, toFloatFunction);
        CubicSpline<TerrainShaper.Point> cubicSpline4 = buildErosionOffsetSpline(-0.05F, 0.03F, 0.1F, 1.0F, 0.01F, 0.01F, true, true, toFloatFunction);
        float f = -0.51F;
        float g = -0.4F;
        float h = 0.1F;
        float i = -0.15F;
        CubicSpline<TerrainShaper.Point> cubicSpline5 = CubicSpline.builder(TerrainShaper.Coordinate.CONTINENTS, toFloatFunction).addPoint(-1.1F, 0.044F, 0.0F).addPoint(-1.02F, -0.2222F, 0.0F).addPoint(-0.51F, -0.2222F, 0.0F).addPoint(-0.44F, -0.12F, 0.0F).addPoint(-0.18F, -0.12F, 0.0F).addPoint(-0.16F, cubicSpline, 0.0F).addPoint(-0.15F, cubicSpline, 0.0F).addPoint(-0.1F, cubicSpline2, 0.0F).addPoint(0.25F, cubicSpline3, 0.0F).addPoint(1.0F, cubicSpline4, 0.0F).build();
        CubicSpline<TerrainShaper.Point> cubicSpline6 = CubicSpline.builder(TerrainShaper.Coordinate.CONTINENTS, NO_TRANSFORM).addPoint(-0.19F, 3.95F, 0.0F).addPoint(-0.15F, getErosionFactor(6.25F, true, NO_TRANSFORM), 0.0F).addPoint(-0.1F, getErosionFactor(5.47F, true, toFloatFunction2), 0.0F).addPoint(0.03F, getErosionFactor(5.08F, true, toFloatFunction2), 0.0F).addPoint(0.06F, getErosionFactor(4.69F, false, toFloatFunction2), 0.0F).build();
        float j = 0.65F;
        CubicSpline<TerrainShaper.Point> cubicSpline7 = CubicSpline.builder(TerrainShaper.Coordinate.CONTINENTS, toFloatFunction3).addPoint(-0.11F, 0.0F, 0.0F).addPoint(0.03F, buildErosionJaggednessSpline(1.0F, 0.5F, 0.0F, 0.0F, toFloatFunction3), 0.0F).addPoint(0.65F, buildErosionJaggednessSpline(1.0F, 1.0F, 1.0F, 0.0F, toFloatFunction3), 0.0F).build();
        return new TerrainShaper(cubicSpline5, cubicSpline6, cubicSpline7);
    }

    private static CubicSpline<TerrainShaper.Point> buildErosionJaggednessSpline(float f, float g, float h, float i, ToFloatFunction<Float> toFloatFunction) {
        float j = -0.5775F;
        CubicSpline<TerrainShaper.Point> cubicSpline = buildRidgeJaggednessSpline(f, h, toFloatFunction);
        CubicSpline<TerrainShaper.Point> cubicSpline2 = buildRidgeJaggednessSpline(g, i, toFloatFunction);
        return CubicSpline.builder(TerrainShaper.Coordinate.EROSION, toFloatFunction).addPoint(-1.0F, cubicSpline, 0.0F).addPoint(-0.78F, cubicSpline2, 0.0F).addPoint(-0.5775F, cubicSpline2, 0.0F).addPoint(-0.375F, 0.0F, 0.0F).build();
    }

    private static CubicSpline<TerrainShaper.Point> buildRidgeJaggednessSpline(float f, float g, ToFloatFunction<Float> toFloatFunction) {
        float h = peaksAndValleys(0.4F);
        float i = peaksAndValleys(0.56666666F);
        float j = (h + i) / 2.0F;
        CubicSpline.Builder<TerrainShaper.Point> builder = CubicSpline.builder(TerrainShaper.Coordinate.RIDGES, toFloatFunction);
        builder.addPoint(h, 0.0F, 0.0F);
        if (g > 0.0F) {
            builder.addPoint(j, buildWeirdnessJaggednessSpline(g, toFloatFunction), 0.0F);
        } else {
            builder.addPoint(j, 0.0F, 0.0F);
        }

        if (f > 0.0F) {
            builder.addPoint(1.0F, buildWeirdnessJaggednessSpline(f, toFloatFunction), 0.0F);
        } else {
            builder.addPoint(1.0F, 0.0F, 0.0F);
        }

        return builder.build();
    }

    private static CubicSpline<TerrainShaper.Point> buildWeirdnessJaggednessSpline(float f, ToFloatFunction<Float> toFloatFunction) {
        float g = 0.63F * f;
        float h = 0.3F * f;
        return CubicSpline.builder(TerrainShaper.Coordinate.WEIRDNESS, toFloatFunction).addPoint(-0.01F, g, 0.0F).addPoint(0.01F, h, 0.0F).build();
    }

    private static CubicSpline<TerrainShaper.Point> getErosionFactor(float value, boolean bl, ToFloatFunction<Float> toFloatFunction) {
        CubicSpline<TerrainShaper.Point> cubicSpline = CubicSpline.builder(TerrainShaper.Coordinate.WEIRDNESS, toFloatFunction).addPoint(-0.2F, 6.3F, 0.0F).addPoint(0.2F, value, 0.0F).build();
        CubicSpline.Builder<TerrainShaper.Point> builder = CubicSpline.builder(TerrainShaper.Coordinate.EROSION, toFloatFunction).addPoint(-0.6F, cubicSpline, 0.0F).addPoint(-0.5F, CubicSpline.builder(TerrainShaper.Coordinate.WEIRDNESS, toFloatFunction).addPoint(-0.05F, 6.3F, 0.0F).addPoint(0.05F, 2.67F, 0.0F).build(), 0.0F).addPoint(-0.35F, cubicSpline, 0.0F).addPoint(-0.25F, cubicSpline, 0.0F).addPoint(-0.1F, CubicSpline.builder(TerrainShaper.Coordinate.WEIRDNESS, toFloatFunction).addPoint(-0.05F, 2.67F, 0.0F).addPoint(0.05F, 6.3F, 0.0F).build(), 0.0F).addPoint(0.03F, cubicSpline, 0.0F);
        if (bl) {
            CubicSpline<TerrainShaper.Point> cubicSpline2 = CubicSpline.builder(TerrainShaper.Coordinate.WEIRDNESS, toFloatFunction).addPoint(0.0F, value, 0.0F).addPoint(0.1F, 0.625F, 0.0F).build();
            CubicSpline<TerrainShaper.Point> cubicSpline3 = CubicSpline.builder(TerrainShaper.Coordinate.RIDGES, toFloatFunction).addPoint(-0.9F, value, 0.0F).addPoint(-0.69F, cubicSpline2, 0.0F).build();
            builder.addPoint(0.35F, value, 0.0F).addPoint(0.45F, cubicSpline3, 0.0F).addPoint(0.55F, cubicSpline3, 0.0F).addPoint(0.62F, value, 0.0F);
        } else {
            CubicSpline<TerrainShaper.Point> cubicSpline4 = CubicSpline.builder(TerrainShaper.Coordinate.RIDGES, toFloatFunction).addPoint(-0.7F, cubicSpline, 0.0F).addPoint(-0.15F, 1.37F, 0.0F).build();
            CubicSpline<TerrainShaper.Point> cubicSpline5 = CubicSpline.builder(TerrainShaper.Coordinate.RIDGES, toFloatFunction).addPoint(0.45F, cubicSpline, 0.0F).addPoint(0.7F, 1.56F, 0.0F).build();
            builder.addPoint(0.05F, cubicSpline5, 0.0F).addPoint(0.4F, cubicSpline5, 0.0F).addPoint(0.45F, cubicSpline4, 0.0F).addPoint(0.55F, cubicSpline4, 0.0F).addPoint(0.58F, value, 0.0F);
        }

        return builder.build();
    }

    private static float calculateSlope(float f, float g, float h, float i) {
        return (g - f) / (i - h);
    }

    private static CubicSpline<TerrainShaper.Point> buildMountainRidgeSplineWithPoints(float f, boolean bl, ToFloatFunction<Float> toFloatFunction) {
        CubicSpline.Builder<TerrainShaper.Point> builder = CubicSpline.builder(TerrainShaper.Coordinate.RIDGES, toFloatFunction);
        float g = -0.7F;
        float h = -1.0F;
        float i = mountainContinentalness(-1.0F, f, -0.7F);
        float j = 1.0F;
        float k = mountainContinentalness(1.0F, f, -0.7F);
        float l = calculateMountainRidgeZeroContinentalnessPoint(f);
        float m = -0.65F;
        if (-0.65F < l && l < 1.0F) {
            float n = mountainContinentalness(-0.65F, f, -0.7F);
            float o = -0.75F;
            float p = mountainContinentalness(-0.75F, f, -0.7F);
            float q = calculateSlope(i, p, -1.0F, -0.75F);
            builder.addPoint(-1.0F, i, q);
            builder.addPoint(-0.75F, p, 0.0F);
            builder.addPoint(-0.65F, n, 0.0F);
            float r = mountainContinentalness(l, f, -0.7F);
            float s = calculateSlope(r, k, l, 1.0F);
            float t = 0.01F;
            builder.addPoint(l - 0.01F, r, 0.0F);
            builder.addPoint(l, r, s);
            builder.addPoint(1.0F, k, s);
        } else {
            float u = calculateSlope(i, k, -1.0F, 1.0F);
            if (bl) {
                builder.addPoint(-1.0F, Math.max(0.2F, i), 0.0F);
                builder.addPoint(0.0F, MathHelper.lerp(0.5F, i, k), u);
            } else {
                builder.addPoint(-1.0F, i, u);
            }

            builder.addPoint(1.0F, k, u);
        }

        return builder.build();
    }

    private static float mountainContinentalness(float weirdness, float continentalness, float weirdnessThreshold) {
        float f = 1.17F;
        float g = 0.46082947F;
        float h = 1.0F - (1.0F - continentalness) * 0.5F;
        float i = 0.5F * (1.0F - continentalness);
        float j = (weirdness + 1.17F) * 0.46082947F;
        float k = j * h - i;
        return weirdness < weirdnessThreshold ? Math.max(k, -0.2222F) : Math.max(k, 0.0F);
    }

    private static float calculateMountainRidgeZeroContinentalnessPoint(float continentalness) {
        float f = 1.17F;
        float g = 0.46082947F;
        float h = 1.0F - (1.0F - continentalness) * 0.5F;
        float i = 0.5F * (1.0F - continentalness);
        return i / (0.46082947F * h) - 1.17F;
    }

    private static CubicSpline<TerrainShaper.Point> buildErosionOffsetSpline(float f, float g, float h, float i, float j, float k, boolean bl, boolean bl2, ToFloatFunction<Float> toFloatFunction) {
        float l = 0.6F;
        float m = 0.5F;
        float n = 0.5F;
        CubicSpline<TerrainShaper.Point> cubicSpline = buildMountainRidgeSplineWithPoints(MathHelper.lerp(i, 0.6F, 1.5F), bl2, toFloatFunction);
        CubicSpline<TerrainShaper.Point> cubicSpline2 = buildMountainRidgeSplineWithPoints(MathHelper.lerp(i, 0.6F, 1.0F), bl2, toFloatFunction);
        CubicSpline<TerrainShaper.Point> cubicSpline3 = buildMountainRidgeSplineWithPoints(i, bl2, toFloatFunction);
        CubicSpline<TerrainShaper.Point> cubicSpline4 = ridgeSpline(f - 0.15F, 0.5F * i, MathHelper.lerp(0.5F, 0.5F, 0.5F) * i, 0.5F * i, 0.6F * i, 0.5F, toFloatFunction);
        CubicSpline<TerrainShaper.Point> cubicSpline5 = ridgeSpline(f, j * i, g * i, 0.5F * i, 0.6F * i, 0.5F, toFloatFunction);
        CubicSpline<TerrainShaper.Point> cubicSpline6 = ridgeSpline(f, j, j, g, h, 0.5F, toFloatFunction);
        CubicSpline<TerrainShaper.Point> cubicSpline7 = ridgeSpline(f, j, j, g, h, 0.5F, toFloatFunction);
        CubicSpline<TerrainShaper.Point> cubicSpline8 = CubicSpline.builder(TerrainShaper.Coordinate.RIDGES, toFloatFunction).addPoint(-1.0F, f, 0.0F).addPoint(-0.4F, cubicSpline6, 0.0F).addPoint(0.0F, h + 0.07F, 0.0F).build();
        CubicSpline<TerrainShaper.Point> cubicSpline9 = ridgeSpline(-0.02F, k, k, g, h, 0.0F, toFloatFunction);
        CubicSpline.Builder<TerrainShaper.Point> builder = CubicSpline.builder(TerrainShaper.Coordinate.EROSION, toFloatFunction).addPoint(-0.85F, cubicSpline, 0.0F).addPoint(-0.7F, cubicSpline2, 0.0F).addPoint(-0.4F, cubicSpline3, 0.0F).addPoint(-0.35F, cubicSpline4, 0.0F).addPoint(-0.1F, cubicSpline5, 0.0F).addPoint(0.2F, cubicSpline6, 0.0F);
        if (bl) {
            builder.addPoint(0.4F, cubicSpline7, 0.0F).addPoint(0.45F, cubicSpline8, 0.0F).addPoint(0.55F, cubicSpline8, 0.0F).addPoint(0.58F, cubicSpline7, 0.0F);
        }

        builder.addPoint(0.7F, cubicSpline9, 0.0F);
        return builder.build();
    }

    private static CubicSpline<TerrainShaper.Point> ridgeSpline(float f, float g, float h, float i, float j, float k, ToFloatFunction<Float> toFloatFunction) {
        float l = Math.max(0.5F * (g - f), k);
        float m = 5.0F * (h - g);
        return CubicSpline.builder(TerrainShaper.Coordinate.RIDGES, toFloatFunction).addPoint(-1.0F, f, l).addPoint(-0.4F, g, Math.min(l, m)).addPoint(0.0F, h, m).addPoint(0.4F, i, 2.0F * (i - h)).addPoint(1.0F, j, 0.7F * (j - i)).build();
    }

    public void addDebugBiomesToVisualizeSplinePoints(Consumer<Pair<Climate.ParameterPoint, ResourceKey<BiomeBase>>> parameters) {
        Climate.Parameter parameter = Climate.Parameter.span(-1.0F, 1.0F);
        parameters.accept(Pair.of(Climate.parameters(parameter, parameter, parameter, parameter, Climate.Parameter.point(0.0F), parameter, 0.01F), Biomes.PLAINS));
        CubicSpline.Multipoint<TerrainShaper.Point> multipoint = (CubicSpline.Multipoint)buildErosionOffsetSpline(-0.15F, 0.0F, 0.0F, 0.1F, 0.0F, -0.03F, false, false, NO_TRANSFORM);
        ResourceKey<BiomeBase> resourceKey = Biomes.DESERT;
        float[] var5 = multipoint.locations();
        int var6 = var5.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            Float float_ = var5[var7];
            parameters.accept(Pair.of(Climate.parameters(parameter, parameter, parameter, Climate.Parameter.point(float_), Climate.Parameter.point(0.0F), parameter, 0.0F), resourceKey));
            resourceKey = resourceKey == Biomes.DESERT ? Biomes.BADLANDS : Biomes.DESERT;
        }

        var5 = ((CubicSpline.Multipoint)this.offsetSampler).locations();
        var6 = var5.length;

        for(int var11 = 0; var11 < var6; ++var11) {
            Float float2 = var5[var11];
            parameters.accept(Pair.of(Climate.parameters(parameter, parameter, Climate.Parameter.point(float2), parameter, Climate.Parameter.point(0.0F), parameter, 0.0F), Biomes.SNOWY_TAIGA));
        }

    }

    @VisibleForDebug
    public CubicSpline<TerrainShaper.Point> offsetSampler() {
        return this.offsetSampler;
    }

    @VisibleForDebug
    public CubicSpline<TerrainShaper.Point> factorSampler() {
        return this.factorSampler;
    }

    @VisibleForDebug
    public CubicSpline<TerrainShaper.Point> jaggednessSampler() {
        return this.jaggednessSampler;
    }

    public float offset(TerrainShaper.Point point) {
        return this.offsetSampler.apply(point) + -0.50375F;
    }

    public float factor(TerrainShaper.Point point) {
        return this.factorSampler.apply(point);
    }

    public float jaggedness(TerrainShaper.Point point) {
        return this.jaggednessSampler.apply(point);
    }

    public TerrainShaper.Point makePoint(float continentalnessNoise, float erosionNoise, float weirdnessNoise) {
        return new TerrainShaper.Point(continentalnessNoise, erosionNoise, peaksAndValleys(weirdnessNoise), weirdnessNoise);
    }

    public static float peaksAndValleys(float weirdness) {
        return -(Math.abs(Math.abs(weirdness) - 0.6666667F) - 0.33333334F) * 3.0F;
    }

    @VisibleForTesting
    protected static enum Coordinate implements INamable, ToFloatFunction<TerrainShaper.Point> {
        CONTINENTS(TerrainShaper.Point::continents, "continents"),
        EROSION(TerrainShaper.Point::erosion, "erosion"),
        WEIRDNESS(TerrainShaper.Point::weirdness, "weirdness"),
        /** @deprecated */
        @Deprecated
        RIDGES(TerrainShaper.Point::ridges, "ridges");

        private static final Map<String, TerrainShaper.Coordinate> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(TerrainShaper.Coordinate::getSerializedName, (coordinate) -> {
            return coordinate;
        }));
        private static final Codec<TerrainShaper.Coordinate> CODEC = INamable.fromEnum(TerrainShaper.Coordinate::values, BY_NAME::get);
        static final Codec<ToFloatFunction<TerrainShaper.Point>> WIDE_CODEC = CODEC.flatComapMap((coordinate) -> {
            return coordinate;
        }, (toFloatFunction) -> {
            DataResult var10000;
            if (toFloatFunction instanceof TerrainShaper.Coordinate) {
                TerrainShaper.Coordinate coordinate = toFloatFunction;
                var10000 = DataResult.success(coordinate);
            } else {
                var10000 = DataResult.error("Not a coordinate resolver: " + toFloatFunction);
            }

            return var10000;
        });
        private final ToFloatFunction<TerrainShaper.Point> reference;
        private final String name;

        private Coordinate(ToFloatFunction<TerrainShaper.Point> noiseFunction, String id) {
            this.reference = noiseFunction;
            this.name = id;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public float apply(TerrainShaper.Point point) {
            return this.reference.apply(point);
        }
    }

    public static record Point(float continents, float erosion, float ridges, float weirdness) {
        public Point(float continentalnessNoise, float erosionNoise, float normalizedWeirdness, float weirdnessNoise) {
            this.continents = continentalnessNoise;
            this.erosion = erosionNoise;
            this.ridges = normalizedWeirdness;
            this.weirdness = weirdnessNoise;
        }

        public float continents() {
            return this.continents;
        }

        public float erosion() {
            return this.erosion;
        }

        public float ridges() {
            return this.ridges;
        }

        public float weirdness() {
            return this.weirdness;
        }
    }
}
