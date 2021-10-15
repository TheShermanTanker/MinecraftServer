package net.minecraft.core;

import com.google.common.collect.Iterators;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3fa;
import com.mojang.math.Vector4f;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.util.INamable;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;

public enum EnumDirection implements INamable {
    DOWN(0, 1, -1, "down", EnumDirection.EnumAxisDirection.NEGATIVE, EnumDirection.EnumAxis.Y, new BaseBlockPosition(0, -1, 0)),
    UP(1, 0, -1, "up", EnumDirection.EnumAxisDirection.POSITIVE, EnumDirection.EnumAxis.Y, new BaseBlockPosition(0, 1, 0)),
    NORTH(2, 3, 2, "north", EnumDirection.EnumAxisDirection.NEGATIVE, EnumDirection.EnumAxis.Z, new BaseBlockPosition(0, 0, -1)),
    SOUTH(3, 2, 0, "south", EnumDirection.EnumAxisDirection.POSITIVE, EnumDirection.EnumAxis.Z, new BaseBlockPosition(0, 0, 1)),
    WEST(4, 5, 1, "west", EnumDirection.EnumAxisDirection.NEGATIVE, EnumDirection.EnumAxis.X, new BaseBlockPosition(-1, 0, 0)),
    EAST(5, 4, 3, "east", EnumDirection.EnumAxisDirection.POSITIVE, EnumDirection.EnumAxis.X, new BaseBlockPosition(1, 0, 0));

    public static final Codec<EnumDirection> CODEC = INamable.fromEnum(EnumDirection::values, EnumDirection::byName);
    private final int data3d;
    private final int oppositeIndex;
    private final int data2d;
    private final String name;
    private final EnumDirection.EnumAxis axis;
    private final EnumDirection.EnumAxisDirection axisDirection;
    private final BaseBlockPosition normal;
    private static final EnumDirection[] VALUES = values();
    private static final Map<String, EnumDirection> BY_NAME = Arrays.stream(VALUES).collect(Collectors.toMap(EnumDirection::getName, (direction) -> {
        return direction;
    }));
    private static final EnumDirection[] BY_3D_DATA = Arrays.stream(VALUES).sorted(Comparator.comparingInt((direction) -> {
        return direction.data3d;
    })).toArray((i) -> {
        return new EnumDirection[i];
    });
    private static final EnumDirection[] BY_2D_DATA = Arrays.stream(VALUES).filter((direction) -> {
        return direction.getAxis().isHorizontal();
    }).sorted(Comparator.comparingInt((direction) -> {
        return direction.data2d;
    })).toArray((i) -> {
        return new EnumDirection[i];
    });
    private static final Long2ObjectMap<EnumDirection> BY_NORMAL = Arrays.stream(VALUES).collect(Collectors.toMap((direction) -> {
        return (new BlockPosition(direction.getNormal())).asLong();
    }, (direction) -> {
        return direction;
    }, (direction1, direction2) -> {
        throw new IllegalArgumentException("Duplicate keys");
    }, Long2ObjectOpenHashMap::new));

    private EnumDirection(int id, int idOpposite, int idHorizontal, String name, EnumDirection.EnumAxisDirection direction, EnumDirection.EnumAxis axis, BaseBlockPosition vector) {
        this.data3d = id;
        this.data2d = idHorizontal;
        this.oppositeIndex = idOpposite;
        this.name = name;
        this.axis = axis;
        this.axisDirection = direction;
        this.normal = vector;
    }

    public static EnumDirection[] orderedByNearest(Entity entity) {
        float f = entity.getViewXRot(1.0F) * ((float)Math.PI / 180F);
        float g = -entity.getViewYRot(1.0F) * ((float)Math.PI / 180F);
        float h = MathHelper.sin(f);
        float i = MathHelper.cos(f);
        float j = MathHelper.sin(g);
        float k = MathHelper.cos(g);
        boolean bl = j > 0.0F;
        boolean bl2 = h < 0.0F;
        boolean bl3 = k > 0.0F;
        float l = bl ? j : -j;
        float m = bl2 ? -h : h;
        float n = bl3 ? k : -k;
        float o = l * i;
        float p = n * i;
        EnumDirection direction = bl ? EAST : WEST;
        EnumDirection direction2 = bl2 ? UP : DOWN;
        EnumDirection direction3 = bl3 ? SOUTH : NORTH;
        if (l > n) {
            if (m > o) {
                return makeDirectionArray(direction2, direction, direction3);
            } else {
                return p > m ? makeDirectionArray(direction, direction3, direction2) : makeDirectionArray(direction, direction2, direction3);
            }
        } else if (m > p) {
            return makeDirectionArray(direction2, direction3, direction);
        } else {
            return o > m ? makeDirectionArray(direction3, direction, direction2) : makeDirectionArray(direction3, direction2, direction);
        }
    }

    private static EnumDirection[] makeDirectionArray(EnumDirection first, EnumDirection second, EnumDirection third) {
        return new EnumDirection[]{first, second, third, third.opposite(), second.opposite(), first.opposite()};
    }

    public static EnumDirection rotate(Matrix4f matrix, EnumDirection direction) {
        BaseBlockPosition vec3i = direction.getNormal();
        Vector4f vector4f = new Vector4f((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ(), 0.0F);
        vector4f.transform(matrix);
        return getNearest(vector4f.x(), vector4f.y(), vector4f.z());
    }

    public Quaternion getRotation() {
        Quaternion quaternion = Vector3fa.XP.rotationDegrees(90.0F);
        switch(this) {
        case DOWN:
            return Vector3fa.XP.rotationDegrees(180.0F);
        case UP:
            return Quaternion.ONE.copy();
        case NORTH:
            quaternion.mul(Vector3fa.ZP.rotationDegrees(180.0F));
            return quaternion;
        case SOUTH:
            return quaternion;
        case WEST:
            quaternion.mul(Vector3fa.ZP.rotationDegrees(90.0F));
            return quaternion;
        case EAST:
        default:
            quaternion.mul(Vector3fa.ZP.rotationDegrees(-90.0F));
            return quaternion;
        }
    }

    public int get3DDataValue() {
        return this.data3d;
    }

    public int get2DRotationValue() {
        return this.data2d;
    }

    public EnumDirection.EnumAxisDirection getAxisDirection() {
        return this.axisDirection;
    }

    public static EnumDirection getFacingAxis(Entity entity, EnumDirection.EnumAxis axis) {
        switch(axis) {
        case X:
            return EAST.isFacingAngle(entity.getViewYRot(1.0F)) ? EAST : WEST;
        case Z:
            return SOUTH.isFacingAngle(entity.getViewYRot(1.0F)) ? SOUTH : NORTH;
        case Y:
        default:
            return entity.getViewXRot(1.0F) < 0.0F ? UP : DOWN;
        }
    }

    public EnumDirection opposite() {
        return fromType1(this.oppositeIndex);
    }

    public EnumDirection getClockWise(EnumDirection.EnumAxis axis) {
        switch(axis) {
        case X:
            if (this != WEST && this != EAST) {
                return this.getClockWiseX();
            }

            return this;
        case Z:
            if (this != NORTH && this != SOUTH) {
                return this.getClockWiseZ();
            }

            return this;
        case Y:
            if (this != UP && this != DOWN) {
                return this.getClockWise();
            }

            return this;
        default:
            throw new IllegalStateException("Unable to get CW facing for axis " + axis);
        }
    }

    public EnumDirection getCounterClockWise(EnumDirection.EnumAxis axis) {
        switch(axis) {
        case X:
            if (this != WEST && this != EAST) {
                return this.getCounterClockWiseX();
            }

            return this;
        case Z:
            if (this != NORTH && this != SOUTH) {
                return this.getCounterClockWiseZ();
            }

            return this;
        case Y:
            if (this != UP && this != DOWN) {
                return this.getCounterClockWise();
            }

            return this;
        default:
            throw new IllegalStateException("Unable to get CW facing for axis " + axis);
        }
    }

    public EnumDirection getClockWise() {
        switch(this) {
        case NORTH:
            return EAST;
        case SOUTH:
            return WEST;
        case WEST:
            return NORTH;
        case EAST:
            return SOUTH;
        default:
            throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
        }
    }

    private EnumDirection getClockWiseX() {
        switch(this) {
        case DOWN:
            return SOUTH;
        case UP:
            return NORTH;
        case NORTH:
            return DOWN;
        case SOUTH:
            return UP;
        default:
            throw new IllegalStateException("Unable to get X-rotated facing of " + this);
        }
    }

    private EnumDirection getCounterClockWiseX() {
        switch(this) {
        case DOWN:
            return NORTH;
        case UP:
            return SOUTH;
        case NORTH:
            return UP;
        case SOUTH:
            return DOWN;
        default:
            throw new IllegalStateException("Unable to get X-rotated facing of " + this);
        }
    }

    private EnumDirection getClockWiseZ() {
        switch(this) {
        case DOWN:
            return WEST;
        case UP:
            return EAST;
        case NORTH:
        case SOUTH:
        default:
            throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
        case WEST:
            return UP;
        case EAST:
            return DOWN;
        }
    }

    private EnumDirection getCounterClockWiseZ() {
        switch(this) {
        case DOWN:
            return EAST;
        case UP:
            return WEST;
        case NORTH:
        case SOUTH:
        default:
            throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
        case WEST:
            return DOWN;
        case EAST:
            return UP;
        }
    }

    public EnumDirection getCounterClockWise() {
        switch(this) {
        case NORTH:
            return WEST;
        case SOUTH:
            return EAST;
        case WEST:
            return SOUTH;
        case EAST:
            return NORTH;
        default:
            throw new IllegalStateException("Unable to get CCW facing of " + this);
        }
    }

    public int getAdjacentX() {
        return this.normal.getX();
    }

    public int getAdjacentY() {
        return this.normal.getY();
    }

    public int getAdjacentZ() {
        return this.normal.getZ();
    }

    public Vector3fa step() {
        return new Vector3fa((float)this.getAdjacentX(), (float)this.getAdjacentY(), (float)this.getAdjacentZ());
    }

    public String getName() {
        return this.name;
    }

    public EnumDirection.EnumAxis getAxis() {
        return this.axis;
    }

    @Nullable
    public static EnumDirection byName(@Nullable String name) {
        return name == null ? null : BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    public static EnumDirection fromType1(int id) {
        return BY_3D_DATA[MathHelper.abs(id % BY_3D_DATA.length)];
    }

    public static EnumDirection fromType2(int value) {
        return BY_2D_DATA[MathHelper.abs(value % BY_2D_DATA.length)];
    }

    @Nullable
    public static EnumDirection fromNormal(BlockPosition pos) {
        return BY_NORMAL.get(pos.asLong());
    }

    @Nullable
    public static EnumDirection fromNormal(int x, int y, int z) {
        return BY_NORMAL.get(BlockPosition.asLong(x, y, z));
    }

    public static EnumDirection fromAngle(double rotation) {
        return fromType2(MathHelper.floor(rotation / 90.0D + 0.5D) & 3);
    }

    public static EnumDirection fromAxisAndDirection(EnumDirection.EnumAxis axis, EnumDirection.EnumAxisDirection direction) {
        switch(axis) {
        case X:
            return direction == EnumDirection.EnumAxisDirection.POSITIVE ? EAST : WEST;
        case Z:
        default:
            return direction == EnumDirection.EnumAxisDirection.POSITIVE ? SOUTH : NORTH;
        case Y:
            return direction == EnumDirection.EnumAxisDirection.POSITIVE ? UP : DOWN;
        }
    }

    public float toYRot() {
        return (float)((this.data2d & 3) * 90);
    }

    public static EnumDirection getRandom(Random random) {
        return SystemUtils.getRandom(VALUES, random);
    }

    public static EnumDirection getNearest(double x, double y, double z) {
        return getNearest((float)x, (float)y, (float)z);
    }

    public static EnumDirection getNearest(float x, float y, float z) {
        EnumDirection direction = NORTH;
        float f = Float.MIN_VALUE;

        for(EnumDirection direction2 : VALUES) {
            float g = x * (float)direction2.normal.getX() + y * (float)direction2.normal.getY() + z * (float)direction2.normal.getZ();
            if (g > f) {
                f = g;
                direction = direction2;
            }
        }

        return direction;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static EnumDirection get(EnumDirection.EnumAxisDirection direction, EnumDirection.EnumAxis axis) {
        for(EnumDirection direction2 : VALUES) {
            if (direction2.getAxisDirection() == direction && direction2.getAxis() == axis) {
                return direction2;
            }
        }

        throw new IllegalArgumentException("No such direction: " + direction + " " + axis);
    }

    public BaseBlockPosition getNormal() {
        return this.normal;
    }

    public boolean isFacingAngle(float f) {
        float g = f * ((float)Math.PI / 180F);
        float h = -MathHelper.sin(g);
        float i = MathHelper.cos(g);
        return (float)this.normal.getX() * h + (float)this.normal.getZ() * i > 0.0F;
    }

    public static enum EnumAxis implements INamable, Predicate<EnumDirection> {
        X("x") {
            @Override
            public int choose(int x, int y, int z) {
                return x;
            }

            @Override
            public double choose(double x, double y, double z) {
                return x;
            }
        },
        Y("y") {
            @Override
            public int choose(int x, int y, int z) {
                return y;
            }

            @Override
            public double choose(double x, double y, double z) {
                return y;
            }
        },
        Z("z") {
            @Override
            public int choose(int x, int y, int z) {
                return z;
            }

            @Override
            public double choose(double x, double y, double z) {
                return z;
            }
        };

        public static final EnumDirection.EnumAxis[] VALUES = values();
        public static final Codec<EnumDirection.EnumAxis> CODEC = INamable.fromEnum(EnumDirection.EnumAxis::values, EnumDirection.EnumAxis::byName);
        private static final Map<String, EnumDirection.EnumAxis> BY_NAME = Arrays.stream(VALUES).collect(Collectors.toMap(EnumDirection.EnumAxis::getName, (axis) -> {
            return axis;
        }));
        private final String name;

        EnumAxis(String string2) {
            this.name = string2;
        }

        @Nullable
        public static EnumDirection.EnumAxis byName(String name) {
            return BY_NAME.get(name.toLowerCase(Locale.ROOT));
        }

        public String getName() {
            return this.name;
        }

        public boolean isVertical() {
            return this == Y;
        }

        public boolean isHorizontal() {
            return this == X || this == Z;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public static EnumDirection.EnumAxis getRandom(Random random) {
            return SystemUtils.getRandom(VALUES, random);
        }

        @Override
        public boolean test(@Nullable EnumDirection direction) {
            return direction != null && direction.getAxis() == this;
        }

        public EnumDirection.EnumDirectionLimit getPlane() {
            switch(this) {
            case X:
            case Z:
                return EnumDirection.EnumDirectionLimit.HORIZONTAL;
            case Y:
                return EnumDirection.EnumDirectionLimit.VERTICAL;
            default:
                throw new Error("Someone's been tampering with the universe!");
            }
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public abstract int choose(int x, int y, int z);

        public abstract double choose(double x, double y, double z);
    }

    public static enum EnumAxisDirection {
        POSITIVE(1, "Towards positive"),
        NEGATIVE(-1, "Towards negative");

        private final int step;
        private final String name;

        private EnumAxisDirection(int offset, String description) {
            this.step = offset;
            this.name = description;
        }

        public int getStep() {
            return this.step;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public EnumDirection.EnumAxisDirection opposite() {
            return this == POSITIVE ? NEGATIVE : POSITIVE;
        }
    }

    public static enum EnumDirectionLimit implements Iterable<EnumDirection>, Predicate<EnumDirection> {
        HORIZONTAL(new EnumDirection[]{EnumDirection.NORTH, EnumDirection.EAST, EnumDirection.SOUTH, EnumDirection.WEST}, new EnumDirection.EnumAxis[]{EnumDirection.EnumAxis.X, EnumDirection.EnumAxis.Z}),
        VERTICAL(new EnumDirection[]{EnumDirection.UP, EnumDirection.DOWN}, new EnumDirection.EnumAxis[]{EnumDirection.EnumAxis.Y});

        private final EnumDirection[] faces;
        private final EnumDirection.EnumAxis[] axis;

        private EnumDirectionLimit(EnumDirection[] facingArray, EnumDirection.EnumAxis[] axisArray) {
            this.faces = facingArray;
            this.axis = axisArray;
        }

        public EnumDirection getRandomDirection(Random random) {
            return SystemUtils.getRandom(this.faces, random);
        }

        public EnumDirection.EnumAxis getRandomAxis(Random random) {
            return SystemUtils.getRandom(this.axis, random);
        }

        @Override
        public boolean test(@Nullable EnumDirection direction) {
            return direction != null && direction.getAxis().getPlane() == this;
        }

        @Override
        public Iterator<EnumDirection> iterator() {
            return Iterators.forArray(this.faces);
        }

        public Stream<EnumDirection> stream() {
            return Arrays.stream(this.faces);
        }
    }
}
