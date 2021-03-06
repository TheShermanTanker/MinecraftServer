package com.mojang.math;

import java.util.Arrays;
import net.minecraft.SystemUtils;

public enum PointGroupS {
    P123(0, 1, 2),
    P213(1, 0, 2),
    P132(0, 2, 1),
    P231(1, 2, 0),
    P312(2, 0, 1),
    P321(2, 1, 0);

    private final int[] permutation;
    private final Matrix3f transformation;
    private static final int ORDER = 3;
    private static final PointGroupS[][] cayleyTable = SystemUtils.make(new PointGroupS[values().length][values().length], (symmetricGroup3s) -> {
        for(PointGroupS symmetricGroup3 : values()) {
            for(PointGroupS symmetricGroup32 : values()) {
                int[] is = new int[3];

                for(int i = 0; i < 3; ++i) {
                    is[i] = symmetricGroup3.permutation[symmetricGroup32.permutation[i]];
                }

                PointGroupS symmetricGroup33 = Arrays.stream(values()).filter((symmetricGroup3x) -> {
                    return Arrays.equals(symmetricGroup3x.permutation, is);
                }).findFirst().get();
                symmetricGroup3s[symmetricGroup3.ordinal()][symmetricGroup32.ordinal()] = symmetricGroup33;
            }
        }

    });

    private PointGroupS(int xMapping, int yMapping, int zMapping) {
        this.permutation = new int[]{xMapping, yMapping, zMapping};
        this.transformation = new Matrix3f();
        this.transformation.set(0, this.permutation(0), 1.0F);
        this.transformation.set(1, this.permutation(1), 1.0F);
        this.transformation.set(2, this.permutation(2), 1.0F);
    }

    public PointGroupS compose(PointGroupS transformation) {
        return cayleyTable[this.ordinal()][transformation.ordinal()];
    }

    public int permutation(int oldAxis) {
        return this.permutation[oldAxis];
    }

    public Matrix3f transformation() {
        return this.transformation;
    }
}
