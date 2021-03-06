package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.levelgen.RandomSource;

public final class NoiseGeneratorPerlin {
    private static final float SHIFT_UP_EPSILON = 1.0E-7F;
    private final byte[] p;
    public final double xo;
    public final double yo;
    public final double zo;

    public NoiseGeneratorPerlin(RandomSource random) {
        this.xo = random.nextDouble() * 256.0D;
        this.yo = random.nextDouble() * 256.0D;
        this.zo = random.nextDouble() * 256.0D;
        this.p = new byte[256];

        for(int i = 0; i < 256; ++i) {
            this.p[i] = (byte)i;
        }

        for(int j = 0; j < 256; ++j) {
            int k = random.nextInt(256 - j);
            byte b = this.p[j];
            this.p[j] = this.p[j + k];
            this.p[j + k] = b;
        }

    }

    public double noise(double x, double y, double z) {
        return this.noise(x, y, z, 0.0D, 0.0D);
    }

    /** @deprecated */
    @Deprecated
    public double noise(double x, double y, double z, double yScale, double yMax) {
        double d = x + this.xo;
        double e = y + this.yo;
        double f = z + this.zo;
        int i = MathHelper.floor(d);
        int j = MathHelper.floor(e);
        int k = MathHelper.floor(f);
        double g = d - (double)i;
        double h = e - (double)j;
        double l = f - (double)k;
        double o;
        if (yScale != 0.0D) {
            double m;
            if (yMax >= 0.0D && yMax < h) {
                m = yMax;
            } else {
                m = h;
            }

            o = (double)MathHelper.floor(m / yScale + (double)1.0E-7F) * yScale;
        } else {
            o = 0.0D;
        }

        return this.sampleAndLerp(i, j, k, g, h - o, l, h);
    }

    public double noiseWithDerivative(double x, double y, double z, double[] ds) {
        double d = x + this.xo;
        double e = y + this.yo;
        double f = z + this.zo;
        int i = MathHelper.floor(d);
        int j = MathHelper.floor(e);
        int k = MathHelper.floor(f);
        double g = d - (double)i;
        double h = e - (double)j;
        double l = f - (double)k;
        return this.sampleWithDerivative(i, j, k, g, h, l, ds);
    }

    private static double gradDot(int hash, double x, double y, double z) {
        return NoiseGenerator3Handler.dot(NoiseGenerator3Handler.GRADIENT[hash & 15], x, y, z);
    }

    private int p(int hash) {
        return this.p[hash & 255] & 255;
    }

    private double sampleAndLerp(int sectionX, int sectionY, int sectionZ, double localX, double localY, double localZ, double fadeLocalX) {
        int i = this.p(sectionX);
        int j = this.p(sectionX + 1);
        int k = this.p(i + sectionY);
        int l = this.p(i + sectionY + 1);
        int m = this.p(j + sectionY);
        int n = this.p(j + sectionY + 1);
        double d = gradDot(this.p(k + sectionZ), localX, localY, localZ);
        double e = gradDot(this.p(m + sectionZ), localX - 1.0D, localY, localZ);
        double f = gradDot(this.p(l + sectionZ), localX, localY - 1.0D, localZ);
        double g = gradDot(this.p(n + sectionZ), localX - 1.0D, localY - 1.0D, localZ);
        double h = gradDot(this.p(k + sectionZ + 1), localX, localY, localZ - 1.0D);
        double o = gradDot(this.p(m + sectionZ + 1), localX - 1.0D, localY, localZ - 1.0D);
        double p = gradDot(this.p(l + sectionZ + 1), localX, localY - 1.0D, localZ - 1.0D);
        double q = gradDot(this.p(n + sectionZ + 1), localX - 1.0D, localY - 1.0D, localZ - 1.0D);
        double r = MathHelper.smoothstep(localX);
        double s = MathHelper.smoothstep(fadeLocalX);
        double t = MathHelper.smoothstep(localZ);
        return MathHelper.lerp3(r, s, t, d, e, f, g, h, o, p, q);
    }

    private double sampleWithDerivative(int sectionX, int sectionY, int sectionZ, double localX, double localY, double localZ, double[] ds) {
        int i = this.p(sectionX);
        int j = this.p(sectionX + 1);
        int k = this.p(i + sectionY);
        int l = this.p(i + sectionY + 1);
        int m = this.p(j + sectionY);
        int n = this.p(j + sectionY + 1);
        int o = this.p(k + sectionZ);
        int p = this.p(m + sectionZ);
        int q = this.p(l + sectionZ);
        int r = this.p(n + sectionZ);
        int s = this.p(k + sectionZ + 1);
        int t = this.p(m + sectionZ + 1);
        int u = this.p(l + sectionZ + 1);
        int v = this.p(n + sectionZ + 1);
        int[] is = NoiseGenerator3Handler.GRADIENT[o & 15];
        int[] js = NoiseGenerator3Handler.GRADIENT[p & 15];
        int[] ks = NoiseGenerator3Handler.GRADIENT[q & 15];
        int[] ls = NoiseGenerator3Handler.GRADIENT[r & 15];
        int[] ms = NoiseGenerator3Handler.GRADIENT[s & 15];
        int[] ns = NoiseGenerator3Handler.GRADIENT[t & 15];
        int[] os = NoiseGenerator3Handler.GRADIENT[u & 15];
        int[] ps = NoiseGenerator3Handler.GRADIENT[v & 15];
        double d = NoiseGenerator3Handler.dot(is, localX, localY, localZ);
        double e = NoiseGenerator3Handler.dot(js, localX - 1.0D, localY, localZ);
        double f = NoiseGenerator3Handler.dot(ks, localX, localY - 1.0D, localZ);
        double g = NoiseGenerator3Handler.dot(ls, localX - 1.0D, localY - 1.0D, localZ);
        double h = NoiseGenerator3Handler.dot(ms, localX, localY, localZ - 1.0D);
        double w = NoiseGenerator3Handler.dot(ns, localX - 1.0D, localY, localZ - 1.0D);
        double x = NoiseGenerator3Handler.dot(os, localX, localY - 1.0D, localZ - 1.0D);
        double y = NoiseGenerator3Handler.dot(ps, localX - 1.0D, localY - 1.0D, localZ - 1.0D);
        double z = MathHelper.smoothstep(localX);
        double aa = MathHelper.smoothstep(localY);
        double ab = MathHelper.smoothstep(localZ);
        double ac = MathHelper.lerp3(z, aa, ab, (double)is[0], (double)js[0], (double)ks[0], (double)ls[0], (double)ms[0], (double)ns[0], (double)os[0], (double)ps[0]);
        double ad = MathHelper.lerp3(z, aa, ab, (double)is[1], (double)js[1], (double)ks[1], (double)ls[1], (double)ms[1], (double)ns[1], (double)os[1], (double)ps[1]);
        double ae = MathHelper.lerp3(z, aa, ab, (double)is[2], (double)js[2], (double)ks[2], (double)ls[2], (double)ms[2], (double)ns[2], (double)os[2], (double)ps[2]);
        double af = MathHelper.lerp2(aa, ab, e - d, g - f, w - h, y - x);
        double ag = MathHelper.lerp2(ab, z, f - d, x - h, g - e, y - w);
        double ah = MathHelper.lerp2(z, aa, h - d, w - e, x - f, y - g);
        double ai = MathHelper.smoothstepDerivative(localX);
        double aj = MathHelper.smoothstepDerivative(localY);
        double ak = MathHelper.smoothstepDerivative(localZ);
        double al = ac + ai * af;
        double am = ad + aj * ag;
        double an = ae + ak * ah;
        ds[0] += al;
        ds[1] += am;
        ds[2] += an;
        return MathHelper.lerp3(z, aa, ab, d, e, f, g, h, w, x, y);
    }

    @VisibleForTesting
    public void parityConfigString(StringBuilder info) {
        NoiseUtils.parityNoiseOctaveConfigString(info, this.xo, this.yo, this.zo, this.p);
    }
}
