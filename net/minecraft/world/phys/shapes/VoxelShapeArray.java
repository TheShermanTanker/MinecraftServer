package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import net.minecraft.SystemUtils;
import net.minecraft.core.EnumDirection;

public class VoxelShapeArray extends VoxelShape {
    private final DoubleList xs;
    private final DoubleList ys;
    private final DoubleList zs;

    protected VoxelShapeArray(VoxelShapeDiscrete shape, double[] xPoints, double[] yPoints, double[] zPoints) {
        this(shape, (DoubleList)DoubleArrayList.wrap(Arrays.copyOf(xPoints, shape.getXSize() + 1)), (DoubleList)DoubleArrayList.wrap(Arrays.copyOf(yPoints, shape.getYSize() + 1)), (DoubleList)DoubleArrayList.wrap(Arrays.copyOf(zPoints, shape.getZSize() + 1)));
    }

    VoxelShapeArray(VoxelShapeDiscrete shape, DoubleList xPoints, DoubleList yPoints, DoubleList zPoints) {
        super(shape);
        int i = shape.getXSize() + 1;
        int j = shape.getYSize() + 1;
        int k = shape.getZSize() + 1;
        if (i == xPoints.size() && j == yPoints.size() && k == zPoints.size()) {
            this.xs = xPoints;
            this.ys = yPoints;
            this.zs = zPoints;
        } else {
            throw (IllegalArgumentException)SystemUtils.pauseInIde(new IllegalArgumentException("Lengths of point arrays must be consistent with the size of the VoxelShape."));
        }
    }

    @Override
    protected DoubleList getCoords(EnumDirection.EnumAxis axis) {
        switch(axis) {
        case X:
            return this.xs;
        case Y:
            return this.ys;
        case Z:
            return this.zs;
        default:
            throw new IllegalArgumentException();
        }
    }
}
