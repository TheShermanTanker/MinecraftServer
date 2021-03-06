package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.util.INamable;

public class BlockStateEnum<T extends Enum<T> & INamable> extends IBlockState<T> {
    private final ImmutableSet<T> values;
    private final Map<String, T> names = Maps.newHashMap();

    protected BlockStateEnum(String name, Class<T> type, Collection<T> values) {
        super(name, type);
        this.values = ImmutableSet.copyOf(values);

        for(T enum_ : values) {
            String string = enum_.getSerializedName();
            if (this.names.containsKey(string)) {
                throw new IllegalArgumentException("Multiple values have the same name '" + string + "'");
            }

            this.names.put(string, enum_);
        }

    }

    @Override
    public Collection<T> getValues() {
        return this.values;
    }

    @Override
    public Optional<T> getValue(String name) {
        return Optional.ofNullable(this.names.get(name));
    }

    @Override
    public String getName(T value) {
        return value.getSerializedName();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object instanceof BlockStateEnum && super.equals(object)) {
            BlockStateEnum<?> enumProperty = (BlockStateEnum)object;
            return this.values.equals(enumProperty.values) && this.names.equals(enumProperty.names);
        } else {
            return false;
        }
    }

    @Override
    public int generateHashCode() {
        int i = super.generateHashCode();
        i = 31 * i + this.values.hashCode();
        return 31 * i + this.names.hashCode();
    }

    public static <T extends Enum<T> & INamable> BlockStateEnum<T> of(String name, Class<T> type) {
        return create(name, type, (enum_) -> {
            return true;
        });
    }

    public static <T extends Enum<T> & INamable> BlockStateEnum<T> create(String name, Class<T> type, Predicate<T> filter) {
        return create(name, type, Arrays.<T>stream(type.getEnumConstants()).filter(filter).collect(Collectors.toList()));
    }

    public static <T extends Enum<T> & INamable> BlockStateEnum<T> of(String name, Class<T> type, T... values) {
        return create(name, type, Lists.newArrayList(values));
    }

    public static <T extends Enum<T> & INamable> BlockStateEnum<T> create(String name, Class<T> type, Collection<T> values) {
        return new BlockStateEnum<>(name, type, values);
    }
}
